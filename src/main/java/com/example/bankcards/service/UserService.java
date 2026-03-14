package com.example.bankcards.service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserDtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;


//public interface UserService extends UserDetailsService {
@Service
@Primary
public class UserService implements UserDetailsService{
//    public UserDetails loadUserByUsername(String username);
//    public boolean existsByUsername(String username);
//    public boolean existsByEmail(String email);
//    public Long getCurrentUserId();


    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.example.bankcards.entity.User user = userRepository.findByUsername(username)
                //.orElseThrow();
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        //.orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));

        //  ВАЖНО: роль должна возвращаться как "ROLE_ADMIN", а не просто "ADMIN"
        //String roleName = "ROLE_" + user.getRole().name();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    public com.example.bankcards.entity.User findByUsername(String username){
        Optional<com.example.bankcards.entity.User> userCard = userRepository.findByUsername(username);
        if (userCard.isEmpty()) throw new ResourceNotFoundException("Пользователь с именем "+username+" не существует");

        return userCard.get();
    }

    public com.example.bankcards.entity.User  findById(Long id) {
        Optional<com.example.bankcards.entity.User> userOptional = userRepository.findById(id);
        if(userOptional.isEmpty()) throw new ResourceNotFoundException("Пользователь с id= "+id+" не существует");

        return userOptional.get();
    }

    public UserDto findByIdDto(Long id) {
        com.example.bankcards.entity.User user = findById(id);

        UserDto userDto = UserDtoMapper.toDto(user);

        return userDto;
    }

    public Page<com.example.bankcards.entity.User> findAll(Pageable pageable){
        return userRepository.findAll(pageable);
    }

    public Page<UserDto> findAllDto(Pageable pageable){
        Page<com.example.bankcards.entity.User> users = userRepository.findAll(pageable);
        Page<UserDto> UsersDto = users.map(UserDtoMapper::toDto);
        return UsersDto;
    }

    public void deleteById(Long userId) {
        userRepository.deleteById(userId);
    }
    //@Override
    public void existsByUsername(String username) {
        if(userRepository.existsByUsername(username)) throw new AlreadyExistsException("Имя пользователя '"+username+"' уже занято");
    }
    //@Override
    public void existsByEmail(String email) {
        if(userRepository.existsByEmail(email)) throw new AlreadyExistsException("Email '"+email+"' уже занято");
    }

    public com.example.bankcards.entity.User save(com.example.bankcards.entity.User entity){
        return userRepository.save(entity);
    }

    /**
     * Извлекает ID текущего аутентифицированного пользователя из SecurityContext.
     *
     * @return ID пользователя
     * @throws RuntimeException если пользователь не аутентифицирован или не найден
     */
    //@Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Пользователь не аутентифицирован");
        }

        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userRepository.findByUsername(username)
                .map(com.example.bankcards.entity.User::getId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }
}