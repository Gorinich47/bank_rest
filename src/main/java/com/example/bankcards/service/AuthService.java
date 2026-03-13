package com.example.bankcards.service;


import com.example.bankcards.dto.LoginRequestDto;
import com.example.bankcards.dto.RegistrationDto;
import com.example.bankcards.dto.TokenResponseDto;
import com.example.bankcards.entity.Token;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InvalidTokenException;
import com.example.bankcards.repository.TokensRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final TokensRepository tokensRepository;


    @Autowired
    public AuthService(AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserService userService, UserRepository userRepository,
                       TokensRepository tokensRepository
                       ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokensRepository = tokensRepository;
    }

    public void register(RegistrationDto request) {


        User user = User.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
    }

    /** Аннулирует все активные токены */
    private void revokeAllToken(User user) {

        List<Token> validTokens = tokensRepository.findAllAccessTokenByUser(user.getId());

        if(!validTokens.isEmpty()){
            validTokens.forEach(t ->{
                t.setLoggedOut(true);
            });
        }

        tokensRepository.saveAll(validTokens);
    }
    /** Сохраняет новый пользовательский токен */
    private void saveUserToken(String accessToken, String refreshToken, User user) {

        Token token = new Token();

        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setLoggedOut(false);
        token.setUser(user);

        tokensRepository.save(token);
    }

    public TokenResponseDto login(LoginRequestDto request) {
        // Аутентифицируем пользователя
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request. getPassword())
        );

        // Загружаем данные пользователя
        UserDetails userDetails = userService.loadUserByUsername(request.getUsername());

        // Генерируем JWT-токены
        String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
        String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

        User user = userRepository
                .findByUsername(userDetails.getUsername())
                .orElseThrow(()->new UsernameNotFoundException("Пользователь "+userDetails.getUsername()+" не зарегистрирован"));
        // Удаление всех старых токена для данного юзера.
        revokeAllToken(user);
        // Сохраняем токен в БД
        saveUserToken(accessToken ,refreshToken, user);
        // Возвращается ответ с новыми токенами
        return new TokenResponseDto(accessToken, refreshToken);
    }

    public void logout(String token) {
        // Помечаем access token как вышедший из системы
        var storedToken = tokensRepository.findByAccessToken(token);
        if (storedToken.isPresent()) {
            storedToken.get().setLoggedOut(true);
            tokensRepository.save(storedToken.get());
        }
    }

    public TokenResponseDto refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        //Извлекаем заголовок авторизации из запроса.
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        //Проверяем, что заголовок авторизации присутствует и начинается с "Bearer ".
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            throw new InvalidTokenException("Недействительный refresh-токен");
        }

        //Извлекаем токен, удаляя префикс "Bearer ".
        String token = authorizationHeader.substring(7);
        //Извлекаем username из токена.
        String username = jwtService.extractUsername(token);
        //Находим пользователя по username. Если такой пользователь не найден, выбрасываем ошибку
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Проверяем, является ли refresh токен действительным для данного пользователя.
        if (jwtService.isValidRefresh(token, username)) {
            // Получаем новые Access/refresh токены
            String accessToken = jwtService.generateAccessToken(username);
            String refreshToken = jwtService.generateRefreshToken(username);
            // Удаляю предыдущие
            revokeAllToken(user);
            // сохраняем новые
            saveUserToken(accessToken, refreshToken, user);
            // Возвращает обновленные токены
            return new TokenResponseDto(accessToken, refreshToken);

        }
        // Если Refresh-Токен недействителен возвращаем 401
        //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        throw new InvalidTokenException("Недействительный refresh-токен");
    }

}