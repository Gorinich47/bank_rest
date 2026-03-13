package com.example.bankcards.service;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.LoginRequestDto;
import com.example.bankcards.dto.RegistrationDto;
import com.example.bankcards.dto.TokenResponseDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.ChecksData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(SecurityConfig .class)
public class AuthServiceTest {

//    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
//    private UserService userService;

    @Autowired
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    //@MockitoBean
    private ChecksData checksData;

    private User user;
    private LoginRequestDto loginRequest;
    private RegistrationDto registrationDto;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .username("Admin")
                .firstName("Иван")
                .lastName("Петров")
                .email("admin@example.com")
                .role(Role.USER)
                .build();

        loginRequest = LoginRequestDto.builder()
                .username("Admin")
                .password("123")
                .build();

        registrationDto = RegistrationDto.builder()
                .username("Admin")
                .firstName("Иван")
                .lastName("Петров")
                .email("ivanpetrov@example.com")
                .password("123")
                .role(Role.USER)
                .build();
    }


    @Test
    void login_InvalidCredentials_ThrowsBadCredentialsException() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void register_ValidData_CreatesUser() {
        // Arrange
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {

            mocked.when(() -> ChecksData.checkRegistrationData(any(RegistrationDto.class)))
                    .thenReturn(null);

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(user);

            // Act
            String responseMessage = "";
            try {
                authService.register(registrationDto);
                responseMessage = "Регистрация прошла успешно";
            } catch (Exception e) {
            }
            ;

            // Assert
            assertEquals("Регистрация прошла успешно", responseMessage);
            verify(userRepository, times(1)).save(any(User.class));
        }
    }

    @Test
    void register_UsernameExists_ThrowsAlreadyExistsException() {
        // Arrange
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {

            mocked.when(() -> ChecksData.checkRegistrationData(any(RegistrationDto.class)))
                    .thenReturn(null);
            //when(checksData.checkRegistrationData(registrationDto)).thenReturn(null);
            when(userRepository.existsByUsername("user")).thenReturn(true);

            // Act & Assert
            assertThrows(AlreadyExistsException.class, () -> {
                authService.register(registrationDto);
            });
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    void register_EmailExists_ThrowsAlreadyExistsException() {
        // Arrange
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {

            mocked.when(() -> ChecksData.checkRegistrationData(any(RegistrationDto.class)))
                    .thenReturn(null);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // Act & Assert
            assertThrows(AlreadyExistsException.class, () -> {
                authService.register(registrationDto);
            });
            verify(userRepository, never()).save(any());
        }
    }


}