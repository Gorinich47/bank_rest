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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
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
@ExtendWith(MockitoExtension.class) // ОБЯЗАТЕЛЬНО
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository; // Добавьте это поле

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CheckService checkService;

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
                .email("ivanpetrov@example.com")
                .password("123")
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
        System.out.println("---------------------------------------------- 0");
        doNothing().when(checkService).checkFields(registrationDto);
        //doNothing().when(userService).existsByUsername("Admin");
        //doNothing().when(userService).existsByEmail("ivanpetrov@example.com");
        System.out.println("---------------------------------------------- 01");
        when(passwordEncoder.encode("123")).thenReturn("encoded-password");
        System.out.println("---------------------------------------------- 02");
        when(userService.save(any(User.class))).thenReturn(user);
        System.out.println("---------------------------------------------- 03");
            // Act
        authService.register(registrationDto);
        System.out.println("---------------------------------------------- 04");
            // Assert
        verify(userService, times(1)).save(any(User.class));

    }

    @Test
    void register_UsernameExists_ThrowsAlreadyExistsException() {
        // Arrange
        doNothing().when(checkService).checkFields(registrationDto);
        doThrow(new DataIntegrityViolationException("Email уже занят"))
                .when(userService).existsByUsername("Admin");
        doNothing().when(userService).existsByEmail("ivanpetrov@example.com");
        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            authService.register(registrationDto);
        });

        verify(userService, never()).save(any());

    }

    @Test
    void register_EmailExists_ThrowsAlreadyExistsException() {
        // Arrange
        System.out.println("---------------------------------------------- 0");
        doNothing().when(checkService).checkFields(any());

        doNothing().when(userService).existsByUsername(anyString());
        System.out.println("---------------------------------------------- 1");
        doThrow(new AlreadyExistsException("Email уже занят"))
                .when(userService).existsByEmail(anyString());
        System.out.println("---------------------------------------------- 2");
        // Act & Assert
        assertThrows(AlreadyExistsException.class, () -> {
                    authService.register(registrationDto);
                });
        System.out.println("---------------------------------------------- 3");
        verify(userService, never()).save(any());

    }


}