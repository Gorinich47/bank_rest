package com.example.bankcards.dto;

import com.example.bankcards.enums.StatusTransaction;
import com.example.bankcards.util.CheckField;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDto {
    //@CheckField
    @NotNull(message = "Сумма перевода не может быть пустой")
    @DecimalMin(value = "0.0", inclusive = true, message = "Сумма перевода должна быть больше 0")
    @Digits(integer = 12, fraction = 2, message = "Неверный формат суммы (макс. 2 знака после запятой)")
    @Positive
    private BigDecimal amount;
    //@CheckField
    @NotNull(message = "ID карты не может быть пустым")
    @Positive(message = "ID карты не может быть отрицательным или равным 0")
    @Digits(integer = 12, fraction = 0, message = "Неверный формат ID карты (не должно быть знака после запятой)")
    private Long senderCardId;
    //@CheckField
    @NotNull(message = "ID карты не может быть пустым")
    @Positive(message = "ID карты не может быть отрицательным или равным 0")
    @Digits(integer = 12, fraction = 0, message = "Неверный формат ID карты (не должно быть знака после запятой)")
    private Long receiverCardId;
    private StatusTransaction status;
    private String description;
}