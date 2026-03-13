package com.example.bankcards.dto;

import com.example.bankcards.enums.StatusTransaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferDto {
    private BigDecimal amount;
    private Long senderCardId;
    private Long receiverCardId;
    private StatusTransaction status;
    private String description;
}