package com.example.bankcards.dto;

import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.util.CheckField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRegistrationDto {
    //@CheckField
    @NotBlank(message = "поле не может быть пустым")
    private String username;
}