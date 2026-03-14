package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.enums.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.*;

import com.example.bankcards.service.*;
import com.example.bankcards.util.UserDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;


import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.example.bankcards.controller.UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTest {

    private static final String ROLE_USER="USER";
    private static final String ROLE_ADMIN="ADMIN";

    @MockitoBean // Создает мок и кладет его в контекст теста
    private  com.example.bankcards.service.JwtService jwtService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    private User user;
    private UserDto userDto;
    private Page<User> userPage;
    private PagedResponse<UserDto> pagedResponse;

    private final MockMvc mockMvc;

    @Autowired
    UserControllerTest(MockMvc mockMvc){
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("admin")
                .firstName("Иван")
                .lastName("Петров")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .username("admin")
                .firstName("Иван")
                .lastName("Петров")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        userPage = new PageImpl<>(java.util.List.of(user), PageRequest.of(0, 10), 1);
        pagedResponse = PagedResponse.fromPage(userPage.map(UserDtoMapper::toDto));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    public void getUserById_ExistingId_ReturnsUserDto() throws Exception {

        when(userService.findById(1L)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    public void getUserById_NonExistingId_ReturnsNotFound() throws Exception {
        // Arrange
        when(userService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Пользователь с id= 999 не существует"));

        // Act & Assert
        mockMvc.perform(get("/api/users/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с id= 999 не существует"));
    }

    @Test
    public void getUserById_UnauthorizedUser_ReturnsForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ReturnsPagedResponse() throws Exception {
        when(userService.findAll(PageRequest.of(0, 10))).thenReturn(userPage);

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("admin"))
                .andExpect(jsonPath("$.content[0].email").value("admin@example.com"))
                .andExpect(jsonPath("$.content[0].firstName").value("Иван"))
                .andExpect(jsonPath("$.content[0].lastName").value("Петров"))
                .andExpect(jsonPath("$.content[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(userService, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    void getAllUsers_UnauthorizedUser_ReturnsPagedResponse() throws Exception {

        when(userService.findAll(PageRequest.of(0, 10))).thenReturn(userPage);

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
  }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_ExistingId_ReturnsNoContent() throws Exception {
        doNothing().when(userService).deleteById(1L);

        mockMvc.perform(delete("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_UnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}