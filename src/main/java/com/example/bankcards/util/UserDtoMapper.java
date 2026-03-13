package com.example.bankcards.util;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.User;

public class UserDtoMapper {

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}
