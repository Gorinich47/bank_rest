package com.example.bankcards.repository;

import com.example.bankcards.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokensRepository extends JpaRepository<Token, Long> {

    // Находит токен по значению access token
    Optional<Token> findByAccessToken(String accessToken);

    // Находит токен по значению refresh token
    Optional<Token> findByRefreshToken(String refreshToken);

    // Находит все активные (не вышедшие из системы) токены пользователя
    List<Token> findByUserAndLoggedOutFalse(Token user);

    // Находит все активные токены пользователя
    @Query("""
            SELECT t FROM Token t inner join User u
            on t.user.id = u.id
            where t.user.id = :userId and t.loggedOut = false
            """)
    List<Token> findAllAccessTokenByUser(Long userId);

    // Проверяет, существует ли активный токен у пользователя
    boolean existsByUserAndLoggedOutFalse(Token user);
}