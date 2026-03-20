package com.example.bankcards.service;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.util.TransactionDtoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class) // ОБЯЗАТЕЛЬНО
public class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private CardRepository cardRepository;

    @MockitoBean
    private CheckService checkService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    private User user;
    private Card senderCard;
    private Card receiverCard;
    private TransferDto transferDto;
    private Transaction transaction;
    private TransactionDto transactionDto;
    private Page<Transaction> transactionPage;

    @BeforeEach
    void setUp() {

        user = User.builder()
                .id(1L)
                .username("Admin")
                .firstName("Иван")
                .lastName("Петров")
                .email("ivanpetrov@example.com")
                .password("123")
                .role(Role.USER)
                .build();

        senderCard = Card.builder()
                .id(1L)
                .number("1000200030004000")
                .user(user)
                .balance(new BigDecimal("1000.0"))
                .status(StatusCard.ACTIVE)
                .build();

        receiverCard = Card.builder()
                .id(2L)
                .user(user)
                .number("5000600070008000")
                .balance(new BigDecimal("500.0"))
                .status(StatusCard.ACTIVE)
                .build();

        transferDto = new TransferDto();
        transferDto.setSenderCardId(1L);
        transferDto.setReceiverCardId(2L);
        transferDto.setAmount(new BigDecimal("100.0"));
        transferDto.setDescription("Перевод между картами");

        transaction = Transaction.builder()
                .id(1L)
                .amount(new BigDecimal("100.0"))
                .description("Перевод между картами")
                .transactionDate(LocalDateTime.now())
                .senderCard(senderCard)
                .receiverCard(receiverCard)
                .build();

        transactionDto = TransactionDtoMapper.toDto(transaction);

        transactionPage = new PageImpl<>(java.util.List.of(transaction), PageRequest.of(0, 10), 1);
    }

    @Test
    void transferBetweenUserCards_ValidData_ReturnsTransactionDto() {
        // Arrange
        when(userService.getCurrentUserId())
                .thenReturn(1L);
        doNothing().when(checkService).checkFields(any());
        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.of(senderCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(java.util.Optional.of(receiverCard));
        when(cardRepository.save(any(Card.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transaction);

        // Act
        TransactionDto result = transactionService.transferBetweenUserCards(transferDto);

        // Assert
        assertNotNull(result);
        assertEquals("100.0", result.getAmount().toString());
        assertEquals("Перевод между картами", result.getDescription());
        verify(cardRepository, times(1)).save(senderCard);
        verify(cardRepository, times(1)).save(receiverCard);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void transferBetweenUserCards_SameCard_ThrowsIllegalArgumentException() {
        // Arrange
        transferDto.setReceiverCardId(1L);
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(java.util.Optional.of(senderCard));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferBetweenUserCards(transferDto);
        });

        assertTrue(exception.getMessage().contains("Нельзя перевести деньги с карты на ту же самую карту"));
    }

    @Test
    void transferBetweenUserCards_CardsNotBelongToUser_ThrowsIllegalArgumentException() {
        // Arrange
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferBetweenUserCards(transferDto);
        });

        assertTrue(exception.getMessage().contains("Карта отправителя не найдена или не принадлежит пользователю"));
    }

    @Test
    void transferBetweenUserCards_InsufficientFunds_ThrowsIllegalArgumentException() {
        // Arrange
        senderCard.setBalance(new BigDecimal("50.0")); // Меньше, чем сумма перевода
        when(userService.getCurrentUserId())
                .thenReturn(1L);
        doNothing().when(checkService).checkFields(any());
        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.of(senderCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(java.util.Optional.of(receiverCard));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferBetweenUserCards(transferDto);
        });

        assertTrue(exception.getMessage().contains("Недостаточно средств на карте"));
    }

    @Test
    void transferBetweenUserCards_ReceiverCardNotActivated_ThrowsAlreadyExistsException() {
        // Arrange
        receiverCard.setStatus(StatusCard.NEW_CARD);
        when(userService.getCurrentUserId())
                .thenReturn(1L);
        doNothing().when(checkService).checkFields(any());
        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.of(senderCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(java.util.Optional.of(receiverCard));

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            transactionService.transferBetweenUserCards(transferDto);
        });

        assertTrue(exception.getMessage().contains("Карта получателя не активирована"));
    }

    @Test
    void transferBetweenUserCards_SenderCardExpired_ThrowsAlreadyExistsException() {
        // Arrange
        senderCard.setStatus(StatusCard.EXPIRED);
        when(userService.getCurrentUserId())
                .thenReturn(1L);
        doNothing().when(checkService).checkFields(any());
        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.of(senderCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(java.util.Optional.of(receiverCard));

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            transactionService.transferBetweenUserCards(transferDto);
        });

        assertTrue(exception.getMessage().contains("У карты срок действия истек"));
    }

    @Test
    void transferBetweenUserCards_ReceiverCardBlocked_ThrowsAlreadyExistsException() {
        // Arrange
        receiverCard.setStatus(StatusCard.BLOCKED);
        when(userService.getCurrentUserId())
                .thenReturn(1L);
        doNothing().when(checkService).checkFields(any());
        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(java.util.Optional.of(senderCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(java.util.Optional.of(receiverCard));

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            transactionService.transferBetweenUserCards(transferDto);
        });

        assertTrue(exception.getMessage().contains("Карта получателя заблокирована"));
    }

    @Test
    void findByUserId_ReturnsPagedTransactions() {
        // Arrange
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(transactionRepository.findBySenderCardUserId(1L, PageRequest.of(0, 10)))
                .thenReturn(transactionPage);

        // Act
        Page<TransactionDto> result = transactionService.findByUserId(1L, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("100.0", result.getContent().get(0).getAmount().toString());
        verify(transactionRepository, times(1)).findBySenderCardUserId(1L, PageRequest.of(0, 10));
    }

    @Test
    void findByDateBetween_ReturnsPagedTransactions() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59);

        when(transactionRepository.findByTransactionDateBetween(start, end, PageRequest.of(0, 10)))
                .thenReturn(transactionPage);

        // Act
        Page<TransactionDto> result = transactionService.findByDateBetween(start, end, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("100.0", result.getContent().get(0).getAmount().toString());
        verify(transactionRepository, times(1)).findByTransactionDateBetween(start, end, PageRequest.of(0, 10));
    }
}