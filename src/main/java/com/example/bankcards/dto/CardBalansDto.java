
package com.example.bankcards.dto;

import com.example.bankcards.enums.StatusCard;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardBalansDto {
    private Long id;
    private String number;
    private LocalDate expiryDate;
    private BigDecimal balance;
}