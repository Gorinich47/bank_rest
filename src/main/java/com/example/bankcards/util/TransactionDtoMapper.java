package com.example.bankcards.util;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.enums.StatusTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class TransactionDtoMapper {

    public static TransactionDto toDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .transactionDate(transaction.getTransactionDate())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .senderCard(CardDtoMapper.toDto(transaction.getSenderCard()))
                .receiverCard(CardDtoMapper.toDto(transaction.getReceiverCard()))
                .description(transaction.getDescription())
                .build();
    }

    public static Transaction toEntity(BigDecimal amount, Card senderCard, Card receiverCard, String comment) {
        return Transaction.builder()
                .transactionDate(LocalDateTime.now())
                .amount(amount)
                .status(StatusTransaction.SUCCESSFUL)
                .senderCard(senderCard)
                .receiverCard(receiverCard)
                .description(comment.isEmpty() ? "Перевод между своими картами": comment)
                .build();
    }

}

