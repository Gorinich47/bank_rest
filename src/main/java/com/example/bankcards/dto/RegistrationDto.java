package com.example.bankcards.dto;

import com.example.bankcards.enums.Role;
import com.example.bankcards.util.CheckField;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDto {
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String username;
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String firstName;
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String lastName;
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    @Email(message = "Некорректный формат электронной почты")
    private String email;
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String password;
    //@CheckField
    @NotNull(message = "поле не может быть пустым")
    private Role role;

}