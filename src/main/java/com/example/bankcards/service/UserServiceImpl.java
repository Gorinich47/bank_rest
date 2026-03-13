package com.example.bankcards.service;

import com.example.bankcards.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//@Service
//@Primary
//public class UserServiceImpl implements UserService{
public class UserServiceImpl{
//    @Autowired
//    private final UserRepository userRepository;
//
//    public UserServiceImpl(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        com.example.bankcards.entity.User user = userRepository.findByUsername(username)
//                //.orElseThrow();
//                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
//
//        //.orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
//
//        //  ВАЖНО: роль должна возвращаться как "ROLE_ADMIN", а не просто "ADMIN"
//        //String roleName = "ROLE_" + user.getRole().name();
//
//        return org.springframework.security.core.userdetails.User
//                .withUsername(user.getUsername())
//                .password(user.getPassword())
//                .authorities("ROLE_" + user.getRole().name())
//                .accountExpired(false)
//                .accountLocked(false)
//                .credentialsExpired(false)
//                .disabled(false)
//                .build();
//    }
//    @Override
//    public boolean existsByUsername(String username) {
//        return userRepository.existsByUsername(username);
//    }
//    @Override
//    public boolean existsByEmail(String email) {
//        return userRepository.existsByEmail(email);
//    }
//
//    /**
//     * Извлекает ID текущего аутентифицированного пользователя из SecurityContext.
//     *
//     * @return ID пользователя
//     * @throws RuntimeException если пользователь не аутентифицирован или не найден
//     */
//    @Override
//    public Long getCurrentUserId() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            throw new RuntimeException("Пользователь не аутентифицирован");
//        }
//
//        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
//        return userRepository.findByUsername(username)
//                .map(com.example.bankcards.entity.User::getId)
//                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
//    }

}
