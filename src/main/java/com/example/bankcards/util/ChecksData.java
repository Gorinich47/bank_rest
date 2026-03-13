package com.example.bankcards.util;

import com.example.bankcards.dto.RegistrationDto;

public class ChecksData {

    // Проверим заполнение полей RegistrationDto
    static public String checkRegistrationData(RegistrationDto registration) {
        if (registration == null) {
            return "registration";
        }
        if (registration.getUsername() == null || registration.getUsername().trim().isEmpty()) {
            return "username";
        }
        if (registration.getEmail() == null || registration.getEmail().trim().isEmpty()) {
            return "email";
        }
        if (registration.getPassword() == null || registration.getPassword().trim().isEmpty()) {
            return "password";
        }
        if (registration.getFirstName() == null || registration.getFirstName().trim().isEmpty()) {
            return "firstName";
        }
        if (registration.getLastName() == null || registration.getLastName().trim().isEmpty()) {
            return "lastName";
        }
        if (registration.getRole() == null || registration.getRole().name().trim().isEmpty()) {
            return "role";
        }
        return null;
    }
}
