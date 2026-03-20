package com.example.bankcards.service;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class) // ОБЯЗАТЕЛЬНО
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDtoMapper userDtoMapper;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .firstName("Иван")
                .lastName("Петров")
                .email("ivanpetrov@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .username("testuser")
                .firstName("Иван")
                .lastName("Петров")
                .email("ivanpetrov@example.com")
                .role(Role.USER)
                .build();
    }

    @Test
    void loadUserByUsername_ValidUsername_ReturnsUserDetails() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("unknown");
        });
    }

    @Test
    void findByUsername_ExistingUsername_ReturnsUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act
        User result = userService.findByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findByUsername_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.findByUsername("unknown");
        });

        assertTrue(exception.getMessage().contains("Пользователь с именем unknown не существует"));
    }

    @Test
    void findById_ExistingId_ReturnsUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        User result = userService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.findById(999L);
        });

        assertTrue(exception.getMessage().contains("Пользователь с id= 999 не существует"));
    }

    @Test
    void findByIdDto_ValidId_ReturnsUserDto() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //when(userDtoMapper.toDto(user)).thenReturn(userDto);

        // Act
        UserDto result = userService.findByIdDto(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findAll_Paged_ReturnsPageOfUsers() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(java.util.List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // Act
        Page<User> result = userService.findAll(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
    }

    @Test
    void findAllDto_Paged_ReturnsPageOfUserDtos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(java.util.List.of(user), pageable, 1);
        Page<UserDto> dtoPage = new PageImpl<>(java.util.List.of(userDto), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        //when(userDtoMapper.toDto(user)).thenReturn(userDto);

        // Act
        Page<UserDto> result = userService.findAllDto(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
    }

    @Test
    void deleteById_ValidId_DeletesUser() {
        // Act
        userService.deleteById(1L);

        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void existsByUsername_ExistingUsername_ThrowsAlreadyExistsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            userService.existsByUsername("testuser");
        });

        assertTrue(exception.getMessage().contains("Имя пользователя 'testuser' уже занято"));
    }

    @Test
    void existsByUsername_NotExists_DoesNotThrow() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> {
            userService.existsByUsername("newuser");
        });
    }

    @Test
    void existsByEmail_ExistingEmail_ThrowsAlreadyExistsException() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            userService.existsByEmail("test@example.com");
        });

        assertTrue(exception.getMessage().contains("Email 'test@example.com' уже занято"));
    }

    @Test
    void existsByEmail_NotExists_DoesNotThrow() {
        // Arrange
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        // Act & Assert
        assertDoesNotThrow(() -> {
            userService.existsByEmail("new@example.com");
        });
    }

    @Test
    void save_ValidUser_ReturnsSavedUser() {
        // Arrange
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User result = userService.save(user);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void getCurrentUserId_AuthenticatedUser_ReturnsUserId() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("pass")
                .roles("USER")
                .build();

        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act
        Long userId = userService.getCurrentUserId();

        // Assert
        assertEquals(1L, userId);
    }

    @Test
    void getCurrentUserId_NotAuthenticated_ThrowsRuntimeException() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.getCurrentUserId();
        });

        assertTrue(exception.getMessage().contains("Пользователь не аутентифицирован"));
    }

    @Test
    void getCurrentUserId_UserNotFound_ThrowsRuntimeException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("unknown")
                .password("pass")
                .roles("USER")
                .build();

        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.getCurrentUserId();
        });

        assertTrue(exception.getMessage().contains("Пользователь не найден: unknown"));
    }
}