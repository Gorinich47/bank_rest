package com.example.bankcards.dto;

import com.example.bankcards.enums.StatusTransaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private LocalDateTime transactionDate;
    private BigDecimal amount;
    private StatusTransaction status;
    private CardDto senderCard;
    private CardDto receiverCard;
    private String description;
}