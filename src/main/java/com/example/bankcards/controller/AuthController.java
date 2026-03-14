package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginRequestDto;
import com.example.bankcards.dto.RegistrationDto;
import com.example.bankcards.dto.RegistrationDtoList;
import com.example.bankcards.dto.TokenResponseDto;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.CheckService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.ChecksData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Контроллер авторизации", description = "Позволяет создать пользователей(пользователей), аутентифицироваться, выходить из системы, обновлять токен")
public class AuthController {

    private final AuthService authService;
    private final CheckService checkDto;

    @Autowired
    public AuthController(AuthService authService, CheckService checkDto) {
        this.authService = authService;
        this.checkDto = checkDto;
    }

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя", description = "Аутентификация пользователя, возвращает JWT-токены")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto LoginRequest) {
        checkDto.checkFields(LoginRequest);
        TokenResponseDto tokenResponseDto = authService.login(LoginRequest);
        return ResponseEntity.ok(tokenResponseDto);
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход пользователя", description = "Желательно использовать для завершения сессии")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registration")
    @Operation(summary = "Регистрация нового пользователя", description = "Регистрация нового пользователя по имени, email и паролю. Можно передать роль, иначе будет присвоена USER")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegistrationDto registrationDto) {

        authService.register(registrationDto);

        return ResponseEntity.ok("Регистрация прошла успешно");
    }

    //@PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/registration_list")
    @Operation(summary = "Регистрация нескольких новых пользователе", description = "Создает пользователй из массива объектов")
    public ResponseEntity<String> registerList(
            @RequestBody @Valid List<RegistrationDto> registrationDtoList) {

        for(var registrationDto : registrationDtoList) {
            authService.register(registrationDto);
        }
        return ResponseEntity.ok("Регистрация прошла успешно");
    }

    @PostMapping("/refresh_token")
    @Operation(summary = "Обновление access токена", description = "По истечении времени жизни доступа выдается новый доступный")
    public ResponseEntity<TokenResponseDto> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        TokenResponseDto tokenResponseDto = authService.refreshToken(request, response);
        return ResponseEntity.ok(tokenResponseDto);
    }
}