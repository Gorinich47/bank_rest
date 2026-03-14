package com.example.bankcards.service;

import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.exception.AlreadyExistsException;
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
    private final CheckService checkService;
    private final UserService userService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, CardRepository cardRepository, UserService userService, CheckService checkService) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.userService = userService;
        this.checkService = checkService;
    }

    @Transactional
    public TransactionDto transferBetweenUserCards(TransferDto transferDto) {

        checkService.checkFields(transferDto);

        Long userId = userService.getCurrentUserId();
        Long fromCardId = transferDto.getSenderCardId();
        Long toCardId = transferDto.getReceiverCardId();
        BigDecimal amount = transferDto.getAmount();
        String comment = transferDto.getDescription()==null ? "" :  transferDto.getDescription();


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

        if (receiverCard.getStatus() == StatusCard.NEW_CARD) {
            throw new AlreadyExistsException("Карта получателя не активирована");
        }

        if (senderCard.getStatus() == StatusCard.EXPIRED) {
            throw new AlreadyExistsException("У карты срок действия истек");
        }

        if (receiverCard.getStatus() == StatusCard.BLOCK_REQUEST || receiverCard.getStatus() == StatusCard.BLOCKED) {
            throw new AlreadyExistsException("Карта получателя заблокирована");
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