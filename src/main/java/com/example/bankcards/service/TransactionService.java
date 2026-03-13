package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;

import com.example.bankcards.util.TransactionDtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, CardRepository cardRepository) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public TransactionDto transferBetweenUserCards(Long userId, Long fromCardId, Long toCardId, BigDecimal amount, String comment) {
        if (fromCardId.equals(toCardId)) {
            throw new IllegalArgumentException("Нельзя перевести деньги с карты на ту же самую карту");
        }

        Card senderCard = cardRepository.findByIdAndUserId(fromCardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Карта отправителя не найдена или не принадлежит пользователю"));

        Card receiverCard = cardRepository.findByIdAndUserId(toCardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Карта получателя не найдена или не принадлежит пользователю"));

        if (senderCard.getBalance() == null || senderCard.getBalance().compareTo(amount)<0) {
            throw new IllegalArgumentException("Недостаточно средств на карте");
        }

        if (senderCard.getStatus() != StatusCard.ACTIVE) {
            throw new IllegalArgumentException("Карта отправителя заблокирована или срок действия истек");
        }

        if (receiverCard.getStatus() != StatusCard.ACTIVE) {
            throw new IllegalArgumentException("Карта получателя заблокирована или срок действия истек");
        }

        // Списание
        senderCard.setBalance(senderCard.getBalance().subtract(amount));
        cardRepository.save(senderCard);

        // Зачисление
        receiverCard.setBalance(receiverCard.getBalance().add(amount));
        cardRepository.save(receiverCard);

        // Создание транзакции
        Transaction transaction = TransactionDtoMapper.toEntity(amount, senderCard, receiverCard, comment);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return TransactionDtoMapper.toDto(savedTransaction);
    }

    public Page<TransactionDto> findByUserId(Long userId, Pageable pageable) {
        return transactionRepository.findBySenderCardUserId(userId, pageable)
                .map(TransactionDtoMapper::toDto);

    }

    public Page<TransactionDto> findByDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        //if (start.is)
        return transactionRepository.findByTransactionDateBetween(start, end, pageable)
                .map(TransactionDtoMapper::toDto);

    }

}