package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.*;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.AlreadyExistsException;
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


import java.util.List;

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

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private com.example.bankcards.service.CheckService checkDto;

    @MockitoBean // Создает мок и кладет его в контекст теста
    private  com.example.bankcards.service.JwtService jwtService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;


    private final MockMvc mockMvc;

    @Autowired
    AuthControllerTest(MockMvc mockMvc){
        this.mockMvc = mockMvc;
    }

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

            doNothing().when(authService).register(registrationDto);

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

    @Test
    void register_UsernameExists_ReturnsConflict() throws Exception {

        // Вернёт new ErrorResponseDTO(HttpStatus.CONFLICT, ex,"Conflict")
        /*
        {
            "details":"Имя пользователя уже занято",
            "error":"Conflict",
            "message":"Имя пользователя уже занято",
            "path":"/api",
            "status":409,
            "timestamp":"2026-03-14T14:54:01.1694515"
        }
         */


        doThrow(new AlreadyExistsException("Имя пользователя уже занято"))
            .when(authService).register(registrationDto);

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Имя пользователя уже занято"));
    }

    @Test
    void register_EmailExists_ReturnsConflict() throws Exception {

        doThrow(new AlreadyExistsException("Email уже занят"))
                .when(authService).register(registrationDto);

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email уже занят"));
    }

    @Test
    void register_EmptyField_ReturnsUnprocessableContent() throws Exception {

        doThrow(new IllegalArgumentException("username: поле не может быть пустым"))
                .when(authService).register(any(RegistrationDto.class));

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
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$[0].status").value(422))
                .andExpect(jsonPath("$[0].message").value("username: поле не может быть пустым"));
    }

    @Test
    void register_WithoutField_ReturnsUnprocessableContent() throws Exception {

            doThrow(new IllegalArgumentException("username: поле не может быть пустым"))
                    .when(authService).register(any(RegistrationDto.class));

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
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$[0].status").value(422))
                    .andExpect(jsonPath("$[0].message").value("username: поле не может быть пустым"));
    }

    @Test
    void registerList_AllValid_ReturnsSuccess() throws Exception {

        List<RegistrationDto> inputList = List.of(registrationDto1, registrationDto2);

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

        verify(authService, times(2)).register(any(RegistrationDto.class));

    }

    @Test
    void registerList_DuplicateUsername_ReturnsBadRequest() throws Exception {

            //doNothing().when(authService).register(registrationDto);

            doThrow(new AlreadyExistsException("Имя пользователя 'user2' уже занято"))
                    .when(authService).register(registrationDto1);

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
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Имя пользователя 'user2' уже занято"));
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
