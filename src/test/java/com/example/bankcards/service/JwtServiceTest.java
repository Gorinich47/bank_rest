package com.example.bankcards.service;

import com.example.bankcards.config.JwtConfig;
import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.entity.Token;
import com.example.bankcards.repository.TokensRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class) // ОБЯЗАТЕЛЬНО
public class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    @MockitoBean
    private TokensRepository tokensRepository;

    @MockitoBean
    private JwtConfig jwtConfig;

    private final String secretKey = "k3R8nh9sZ7v5wWmQ2xP1aB6cD4eF7gH0iJ9kL3mN5oQ=";
    private final String username = "testuser";
    private final long accessTokenExpireTime = 3600000; // 1 час
    private final long refreshTokenExpireTime = 86400000; // 24 часа

    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecretKey()).thenReturn(secretKey);
        when(jwtConfig.getIssuer()).thenReturn("check-app");
        when(jwtConfig.getAccessTokenExpireTime()).thenReturn(accessTokenExpireTime);
        when(jwtConfig.getRefreshTokenExpireTime()).thenReturn(refreshTokenExpireTime);

        //signingKey = Jwts.SIG.HS256.key().build();
        // Вместо генерации рандомного ключа, создаем его из вашей строки secretKey
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void generateAccessToken_ReturnsValidToken() {
        // Act
        String token = jwtService.generateAccessToken(username);

        // Parse and validate
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Assert
        assertNotNull(token);
        assertEquals(username, claims.getSubject());
        assertEquals("check-app", claims.getIssuer());
        assertFalse(claims.getExpiration().before(new Date()));
    }

    @Test
    void generateRefreshToken_ReturnsValidToken() {
        // Act
        String token = jwtService.generateRefreshToken(username);

        // Parse and validate
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Assert
        assertNotNull(token);
        assertEquals(username, claims.getSubject());
        assertEquals("check-app", claims.getIssuer());
        long expectedExpiration = System.currentTimeMillis() + refreshTokenExpireTime;
        long actualExpiration = claims.getExpiration().getTime();
        assertTrue(Math.abs(expectedExpiration - actualExpiration) < 1000); // погрешность < 1 сек
    }

    @Test
    void extractUsername_ReturnsCorrectUsername() {
        // Arrange
        String token = Jwts.builder()
                .subject(username)
                .issuer("check-app")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpireTime))
                .signWith(signingKey)
                .compact();

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals(username, extractedUsername);
    }

    @Test
    void extractExpiration_ReturnsCorrectExpiration() {
        // Arrange
        // Обнуляем миллисекунды сразу при создании
        long currentTimeMillis = System.currentTimeMillis();
        long expirationMillis = (currentTimeMillis + accessTokenExpireTime) / 1000 * 1000;
        Date expectedExpiration = new Date(expirationMillis);

        String token = Jwts.builder()
                .subject(username)
                .issuer("check-app")
                .issuedAt(new Date())
                .expiration(expectedExpiration)
                .signWith(signingKey)
                .compact();

        // Act
        Date expiration = jwtService.extractExpiration(token);

        // Assert
        assertEquals(expectedExpiration.getTime(), expiration.getTime());
    }

    @Test
    void isValidToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtService.generateAccessToken(username);

        Token storedToken = new Token();
        storedToken.setAccessToken(token);
        storedToken.setLoggedOut(false);

        when(tokensRepository.findByAccessToken(token)).thenReturn(Optional.of(storedToken));

        // Act
        boolean isValid = jwtService.isValidToken(token, username);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void isValidToken_UsernameMismatch_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateAccessToken("otheruser");

        when(tokensRepository.findByAccessToken(token)).thenReturn(Optional.empty());

        // Act
        boolean isValid = jwtService.isValidToken(token, username);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isValidToken_ExpiredToken_ReturnsFalse() {
        // Arrange
        String token = Jwts.builder()
                .subject(username)
                .issuer("check-app")
                .issuedAt(new Date(System.currentTimeMillis() - 2 * accessTokenExpireTime))
                .expiration(new Date(System.currentTimeMillis() - accessTokenExpireTime))
                .signWith(signingKey)
                .compact();

        when(tokensRepository.findByAccessToken(token)).thenReturn(Optional.empty());

        // Act
        boolean isValid = jwtService.isValidToken(token, username);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isValidToken_LoggedOutToken_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateAccessToken(username);

        Token storedToken = new Token();
        storedToken.setAccessToken(token);
        storedToken.setLoggedOut(true); // Токен был отозван

        when(tokensRepository.findByAccessToken(token)).thenReturn(Optional.of(storedToken));

        // Act
        boolean isValid = jwtService.isValidToken(token, username);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void isValidRefresh_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtService.generateRefreshToken(username);

        Token storedToken = new Token();
        storedToken.setRefreshToken(token);
        storedToken.setLoggedOut(false);

        when(tokensRepository.findByRefreshToken(token)).thenReturn(Optional.of(storedToken));

        // Act
        boolean isValid = jwtService.isValidRefresh(token, username);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void isValidRefresh_LoggedOutRefreshToken_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateRefreshToken(username);

        Token storedToken = new Token();
        storedToken.setRefreshToken(token);
        storedToken.setLoggedOut(true);

        when(tokensRepository.findByRefreshToken(token)).thenReturn(Optional.of(storedToken));

        // Act
        boolean isValid = jwtService.isValidRefresh(token, username);

        // Assert
        assertFalse(isValid);
    }
}