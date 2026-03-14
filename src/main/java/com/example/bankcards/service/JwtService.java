package com.example.bankcards.service;


import com.example.bankcards.config.JwtConfig;
import com.example.bankcards.repository.TokensRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final TokensRepository tokensRepository;

    @Autowired
    public JwtService(TokensRepository tokensRepository, JwtConfig jwtConfig) {
        this.tokensRepository = tokensRepository;
        this.jwtConfig = jwtConfig;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(jwtConfig.getSecretKey()); //декодируем секретный ключ, из формата Base64URL в массив байтов.
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, long expiryTime) {
        return Jwts.builder()
                .subject(username)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiryTime))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                //.signWith(getSigningKey())
                .compact();
    }
    /** Метод для генерации access токена */
    public String generateAccessToken(String username) {
        return generateToken(username, jwtConfig.getAccessTokenExpireTime());
    }

    /** Метод для создания Refresh токены */
    public String generateRefreshToken(String username) {
        return generateToken(username, jwtConfig.getRefreshTokenExpireTime());
    }

    /** метод для извлечения имени пользователя */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    /** метод для проверки даты истечения */
    public Date extractExpiration(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }
    /** метод для проверки просрочки токена */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    /** токен действующий */
    public boolean isValidToken(String token, String username) {
        // извлекаем имя из токена
        final String usernameToken = extractUsername(token);
        // проверяем, существует ли токен в репозитории и не был ли он отмечен как "вышедший".
        boolean isValidToken = tokensRepository.findByAccessToken(token)
                .map(t -> !t.isLoggedOut()).orElse(false);
        // проверям и возвращаем результат
        return (usernameToken.equals(username) /* имя совпадает */
                && !isTokenExpired(token) /* токен не просрочился*/
                && isValidToken); /*текущий токен еще действует*/
    }
    /** рефреш токен действующий */
    public boolean isValidRefresh(String token, String username) {
        // извлекаем имя из токена
        String usernameToken = extractUsername(token);
        // проверяем, существует ли токен в репозитории и не был ли он отмечен как "вышедший".
        boolean isValidRefreshToken = tokensRepository.findByRefreshToken(token)
                .map(t -> !t.isLoggedOut()).orElse(false);
        // проверям и возвращаем результат
        return usernameToken.equals(username)
                && !isTokenExpired(token)
                && isValidRefreshToken;
    }

}
