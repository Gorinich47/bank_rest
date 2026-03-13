package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.*;
import com.example.bankcards.enums.Role;
import com.example.bankcards.service.*;
import com.example.bankcards.util.ChecksData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.example.bankcards.controller.AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    private static final String ROLE_USER="USER";
    private static final String ROLE_ADMIN="ADMIN";

    //@MockitoBean
    private ChecksData checksData;

    @MockitoBean
    private AuthService authService;

    @MockitoBean // Создает мок и кладет его в контекст теста
    private  com.example.bankcards.service.JwtService jwtService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    @Autowired
    private MockMvc mockMvc;

    private LoginRequestDto loginRequest;
    private RegistrationDto registrationDto;
    private RegistrationDto registrationDto1;
    private RegistrationDto registrationDto2;
    private TokenResponseDto tokenResponse;

    @BeforeEach
    void setUp() {

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
                .role(Role.ADMIN)
                .build();

        registrationDto1 = RegistrationDto.builder()
                .username("user1")
                .firstName("Иван1")
                .lastName("Петров1")
                .email("ivanpetrov1@example.com")
                .password("1234")
                .role(Role.USER)
                .build();

        registrationDto2 = RegistrationDto.builder()
                .username("user2")
                .firstName("Иван2")
                .lastName("Петров2")
                .email("ivanpetrov2@example.com")
                .password("1234")
                .role(Role.USER)
                .build();

        tokenResponse = TokenResponseDto.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
    }

    @Test
    void login_ValidCredentials_ReturnsTokenResponse() throws Exception {
        when(authService.login(loginRequest)).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "Admin",
                                    "password": "123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        verify(authService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    void logout_ValidToken_ReturnsNoContent() throws Exception {
        String authHeader = "Bearer valid-token";

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        verify(authService, times(1)).logout("valid-token");
    }

    @Test
    void logout_NoAuthHeader_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isInternalServerError());

        verify(authService, never()).logout(anyString());
    }

    @Test
    void register_ValidData_ReturnsSuccess() throws Exception {
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {
            mocked.when(() -> ChecksData.checkRegistrationData(any(RegistrationDto.class)))
                    .thenReturn(null);
            when(userService.existsByUsername("Admin")).thenReturn(false);
            when(userService.existsByEmail("ivanpetrov@example.com")).thenReturn(false);
            //when(ChecksData.checkRegistrationData(registrationDto)).thenReturn(null);

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "username": "Admin",
                                        "firstName": "Иван",
                                        "lastName": "Петров",
                                        "email": "ivanpetrov@example.com",
                                        "password": "123",
                                        "role": "ADMIN"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Регистрация прошла успешно"));

            verify(authService, times(1)).register(any(RegistrationDto.class));
        }
    }

    @Test
    void register_UsernameExists_ReturnsBadRequest() throws Exception {
        when(userService.existsByUsername("Admin")).thenReturn(true);

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "Admin",
                                    "firstName": "Иван",
                                    "lastName": "Петров",
                                    "email": "ivanpetrov@example.com",
                                    "password": "123",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Имя пользователя уже занято"));
    }

    @Test
    void register_EmailExists_ReturnsBadRequest() throws Exception {
        when(userService.existsByUsername("Admin")).thenReturn(false);
        when(userService.existsByEmail("ivanpetrov@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "Admin",
                                    "firstName": "Иван",
                                    "lastName": "Петров",
                                    "email": "ivanpetrov@example.com",
                                    "password": "123",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email уже занят"));
    }

    @Test
    void register_EmptyField_ReturnsBadRequest() throws Exception {
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {
            mocked.when(() -> ChecksData.checkRegistrationData(any(RegistrationDto.class)))
                    .thenReturn("username");

        when(userService.existsByUsername("Admin")).thenReturn(false);
        when(userService.existsByEmail("ivanpetrov@example.com")).thenReturn(false);
        //when(ChecksData.checkRegistrationData(any(RegistrationDto.class))).thenReturn("username");

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "",
                                    "firstName": "Иван",
                                    "lastName": "Петров",
                                    "email": "ivanpetrov@example.com",
                                    "password": "123",
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Поле: 'username' не может быть пустым"));
        }
    }

    @Test
    void register_WithoutField_ReturnsBadRequest() throws Exception {
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {
            mocked.when(() -> ChecksData.checkRegistrationData(any(RegistrationDto.class)))
                    .thenReturn("username");

            when(userService.existsByUsername("Admin")).thenReturn(false);
            when(userService.existsByEmail("ivanpetrov@example.com")).thenReturn(false);
            //when(ChecksData.checkRegistrationData(any(RegistrationDto.class))).thenReturn("username");

            mockMvc.perform(post("/api/auth/registration")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "firstName": "Иван",
                                        "lastName": "Петров",
                                        "email": "ivanpetrov@example.com",
                                        "password": "123",
                                        "role": "ADMIN"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Поле: 'username' не может быть пустым"));
        }
    }

    @Test
    void registerList_AllValid_ReturnsSuccess() throws Exception {
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {

            when(userService.existsByUsername("user1")).thenReturn(false);
            when(userService.existsByEmail("ivanpetrov2@example.com")).thenReturn(false);
            //when(ChecksData.checkRegistrationData(registrationDto1)).thenReturn(null);
            mocked.when(()->ChecksData.checkRegistrationData(registrationDto1)).thenReturn(null);

            when(userService.existsByUsername("user2")).thenReturn(false);
            when(userService.existsByEmail("ivanpetrov1@example.com")).thenReturn(false);
            //when(ChecksData.checkRegistrationData(registrationDto2)).thenReturn(null);
            mocked.when(()->ChecksData.checkRegistrationData(registrationDto2)).thenReturn(null);

            mockMvc.perform(post("/api/auth/registration_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [
                                        {
                                            "username": "user1",
                                            "firstName": "Иван1",
                                            "lastName": "Петров1",
                                            "email": "ivanpetrov1@example.com",
                                            "password": "1234",
                                             "role": "USER"
                                        },
                                        {
                                            "username": "user2",
                                            "firstName": "Иван2",
                                            "lastName": "Петров2",
                                            "email": "ivanpetrov2@example.com",
                                            "password": "1234",
                                            "role": "USER"
                                        }
                                    ]
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Регистрация прошла успешно"));

            verify(authService, times(1)).register(eq(registrationDto1));
            verify(authService, times(1)).register(eq(registrationDto2));
        }
    }

    @Test
    void registerList_DuplicateUsername_ReturnsBadRequest() throws Exception {
        try (MockedStatic<ChecksData> mocked = Mockito.mockStatic(ChecksData.class)) {

            when(userService.existsByUsername("user1")).thenReturn(false);
            when(userService.existsByEmail("user1@example.com")).thenReturn(false);
            //when(ChecksData.checkRegistrationData(registrationDto1)).thenReturn(null);
            mocked.when(()->ChecksData.checkRegistrationData(registrationDto1)).thenReturn(null);

            when(userService.existsByUsername("user2")).thenReturn(true); // Уже существует

            mockMvc.perform(post("/api/auth/registration_list")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    [
                                        {
                                            "username": "user1",
                                            "firstName": "Иван1",
                                            "lastName": "Петров1",
                                            "email": "ivanpetrov1@example.com",
                                            "password": "1234",
                                             "role": "USER"
                                        },
                                        {
                                            "username": "user2",
                                            "firstName": "Иван2",
                                            "lastName": "Петров2",
                                            "email": "ivanpetrov2@example.com",
                                            "password": "1234",
                                            "role": "USER"
                                        }
                                    ]
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Имя пользователя уже занято"));
        }
    }

    @Test
    void refreshToken_ValidRequest_ReturnsNewTokens() throws Exception {
        when(authService.refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

}
