package com.example.bankcards.service;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.CardBalansDto;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRegistrationDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(SecurityConfig.class)
@ExtendWith(MockitoExtension.class) // ОБЯЗАТЕЛЬНО
public class CardServiceTest {

    @InjectMocks
    private CardService cardService;

    @MockitoBean
    private CardRepository cardRepository;

    @MockitoBean
    private CheckService checkService;

    @MockitoBean(name = "userService")
    private UserService userService;

    private User user;
    private Card card;
    private CardDto cardDto;
    private CardRegistrationDto registrationDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .role(com.example.bankcards.enums.Role.USER)
                .build();

        card = Card.builder()
                .id(1L)
                .number("1000200030004000")
                .balance(BigDecimal.valueOf(1000))
                .status(StatusCard.NEW_CARD)
                .user(user)
                .build();

        cardDto = CardDto.builder()
                .id(1L)
                .number("**** **** **** 4000")
                .balance(BigDecimal.valueOf(1000))
                .status(StatusCard.NEW_CARD)
                .user("testuser")
                .build();

        registrationDto = new CardRegistrationDto();
        registrationDto.setUsername("testuser");
    }

    @Test
    void findAllDto_ReturnsPagedCardDtos() {
        // Arrange
        Page<Card> cardPage = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);
        when(cardRepository.findAll(PageRequest.of(0, 10))).thenReturn(cardPage);

        // Act
        Page<CardDto> result = cardService.findAllDto(PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("**** **** **** 4000", result.getContent().get(0).getNumber());
        verify(cardRepository, times(1)).findAll(PageRequest.of(0, 10));
    }

    @Test
    void findByStatus_ReturnsCardsWithStatus() {
        // Arrange
        card.setStatus(StatusCard.BLOCK_REQUEST);
        Page<Card> cardPage = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);
        when(cardRepository.findByStatusIn(List.of(StatusCard.BLOCK_REQUEST), PageRequest.of(0, 10)))
                .thenReturn(cardPage);

        // Act
        Page<CardDto> result = cardService.findByStatus(StatusCard.BLOCK_REQUEST, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(StatusCard.BLOCK_REQUEST, result.getContent().get(0).getStatus());
        verify(cardRepository, times(1)).findByStatusIn(List.of(StatusCard.BLOCK_REQUEST), PageRequest.of(0, 10));
    }

    @Test
    void findByUserIdAndNumberContaining_NoSearchNumber_ReturnsAllUserCards() {
        // Arrange
        Page<Card> cardPage = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByUserId(1L, PageRequest.of(0, 10))).thenReturn(cardPage);

        // Act
        Page<CardDto> result = cardService.findByUserIdAndNumberContaining(null, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(cardRepository, times(1)).findByUserId(1L, PageRequest.of(0, 10));
    }

    @Test
    void findByUserIdAndNumberContaining_WithSearchNumber_ReturnsFilteredCards() {
        // Arrange
        Page<Card> cardPage = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByUserIdAndNumberContaining(1L, "1234", PageRequest.of(0, 10)))
                .thenReturn(cardPage);

        // Act
        Page<CardDto> result = cardService.findByUserIdAndNumberContaining("1234", PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(cardRepository, times(1)).findByUserIdAndNumberContaining(1L, "1234", PageRequest.of(0, 10));
    }

    @Test
    void blockRequestCardForUser_ValidCard_ChangesStatusToBlockRequest() {
        // Arrange
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);

        // Act
        CardDto result = cardService.blockRequestCardForUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(StatusCard.BLOCK_REQUEST, result.getStatus());
        assertEquals(StatusCard.BLOCK_REQUEST, card.getStatus());
        verify(cardRepository, times(1)).save(card);
    }

    @Test
    void blockRequestCardForUser_CardAlreadyBlocked_ThrowsAlreadyExistsException() {
        // Arrange
        card.setStatus(StatusCard.BLOCKED);
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(card));

        // Act & Assert
        assertThrows(AlreadyExistsException.class, () -> {
            cardService.blockRequestCardForUser(1L);
        });
    }

    @Test
    void blockRequestCardForUser_BlockRequestAlreadySent_ThrowsAlreadyExistsException() {
        // Arrange
        card.setStatus(StatusCard.BLOCK_REQUEST);
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(card));

        // Act & Assert
        assertThrows(AlreadyExistsException.class, () -> {
            cardService.blockRequestCardForUser(1L);
        });
    }

    @Test
    void blockAllRequestCardForUser_CallsRepositoryUpdate() {
        // Act
        cardService.blockAllRequestCardForUser();

        // Assert
        verify(cardRepository, times(1)).updateByStatusIn(
                List.of(StatusCard.BLOCK_REQUEST),
                StatusCard.BLOCKED
        );
    }

    @Test
    void blockCardForUser_ValidCard_ChangesStatusToBlocked() {
        // Arrange
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);

        // Act
        CardDto result = cardService.blockCardForUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(StatusCard.BLOCKED, result.getStatus());
        verify(cardRepository, times(1)).save(card);
    }

    @Test
    void blockCardForUser_AlreadyBlocked_ThrowsAlreadyExistsException() {
        // Arrange
        card.setStatus(StatusCard.BLOCKED);
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // Act & Assert
        assertThrows(AlreadyExistsException.class, () -> {
            cardService.blockCardForUser(1L);
        });
    }

    @Test
    void activateCard_ValidCard_ChangesStatusToActive() {
        // Arrange
        card.setStatus(StatusCard.NEW_CARD);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);

        // Act
        CardDto result = cardService.activateCard(1L);

        // Assert
        assertNotNull(result);
        assertEquals(StatusCard.ACTIVE, result.getStatus());
        verify(cardRepository, times(1)).save(card);
    }

    @Test
    void activateCard_AlreadyActive_ThrowsAlreadyExistsException() {
        // Arrange
        card.setStatus(StatusCard.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            cardService.activateCard(1L);
        });

        assertTrue(exception.getMessage().contains("уже активная"));
    }

    @Test
    void activateCard_ExpiredCard_ThrowsAlreadyExistsException() {
        // Arrange
        card.setStatus(StatusCard.EXPIRED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // Act & Assert
        Exception exception = assertThrows(AlreadyExistsException.class, () -> {
            cardService.activateCard(1L);
        });

        assertTrue(exception.getMessage().contains("истек срок действия"));
    }

    @Test
    void activateCards_MultipleValidCards_ActivatesAll() {
        // Arrange
        Card card1 = Card.builder().id(1L).user(user).status(StatusCard.NEW_CARD).build();
        Card card2 = Card.builder().id(2L).user(user).status(StatusCard.NEW_CARD).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card1));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(card2));
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        List<CardDto> result = cardService.activateCards(List.of(1L, 2L));

        // Assert
        assertEquals(2, result.size());
        assertEquals(StatusCard.ACTIVE, result.get(0).getStatus());
        assertEquals(StatusCard.ACTIVE, result.get(1).getStatus());
    }

    @Test
    void createCard_ValidData_ReturnsCreatedCard() {
        // Arrange
        doNothing().when(checkService).checkFields(registrationDto);
        when(userService.findByUsername("testuser")).thenReturn(user);
        when(cardRepository.save(any(Card.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        CardDto result = cardService.createCard(registrationDto);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUser());
        assertEquals(StatusCard.NEW_CARD, result.getStatus());
        verify(cardRepository, times(1)).save(any(Card.class));
    }

    @Test
    void deleteCard_ExistingId_DeletesCard() {
        // Arrange
        when(cardRepository.existsById(1L)).thenReturn(true);
        doNothing().when(cardRepository).deleteById(1L);

        // Act
        cardService.deleteCard(1L);

        // Assert
        verify(cardRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteCard_NonExistingId_ThrowsIllegalArgumentException() {
        // Arrange
        when(cardRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cardService.deleteCard(999L);
        });
    }

    @Test
    void findByIdForUser_ValidId_ReturnsBalanceDto() {
        // Arrange
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(card));

        // Act
        CardBalansDto result = cardService.findByIdForUser(1L);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(1000), result.getBalance());
    }

    @Test
    void findByIdForUser_CardNotOwned_ThrowsIllegalArgumentException() {
        // Arrange
        when(userService.getCurrentUserId()).thenReturn(1L);
        when(cardRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            cardService.findByIdForUser(1L);
        });
    }
}