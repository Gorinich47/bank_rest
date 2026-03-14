package com.example.bankcards.dto;

import com.example.bankcards.util.CheckField;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String username;
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String password;
}