package com.example.bankcards.security;

import com.example.bankcards.service.JwtService;
import com.example.bankcards.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    //@Autowired(required=true)
    private JwtService jwtService;

   // @Autowired(required=true)
    private UserService userService;

    @Autowired(required=true)
    JwtAuthenticationFilter(JwtService jwtService, UserService userService){
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String header = request.getHeader("Authorization"); //извлекаем значение заголовка Authorization из HTTP-запроса.
        //проверяем, существует ли заголовок авторизации и начинается ли он с "Bearer ".
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        //извлекаем сам токен, удаляя префикс "Bearer " из заголовка.
        final String token = header.substring(7);
        //извлекаем username, закодированное в токене, используя наш сервис jwtService
        final String username = jwtService.extractUsername(token);
        //проверяем, что пользователь еще не аутентифицирован в текущем контексте безопасности
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(username); //загружаем данные пользователя из сервиса userService.
            //проверяем, что токен действителен.
            if (jwtService.isValidToken(token, userDetails.getUsername())) {
                //создаем объект, который содержит информацию о пользователе и его полномочия.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                //добавляем дополнительные детали из текущего запроса к объекту аутентификации.
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                //устанавливаем созданный аутентификационный токен в текущий контекст безопасности, тем самым аутентифицируя пользователя в системе.
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        //передаем управление следующему фильтру в цепочке.
        filterChain.doFilter(request, response);
    }
}