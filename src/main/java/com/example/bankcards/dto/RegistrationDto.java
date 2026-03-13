package com.example.bankcards.dto;

import com.example.bankcards.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDto {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;

}