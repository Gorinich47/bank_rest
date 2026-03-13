package com.example.bankcards.controller;

import com.example.bankcards.dto.CardBalansDto;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRegistrationDto;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.CardDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/cards")
@Tag(name = "Контроллер карт", description = "Управление картами")
public class CardController {

    private final CardService cardService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public CardController(CardService cardService, UserService userService, UserRepository userRepository) {
        this.cardService = cardService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // Пользователь: просматривает свои карты с пагинацией и поиском
    @GetMapping("/my")
    @Operation(summary = "Пользователь: Карты пользователя", description = "Пользователь может получить список своих карт")
    public ResponseEntity<PagedResponse<CardDto>> getMyCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String number) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.findByUserIdAndNumberContaining(userService.getCurrentUserId(), number, pageable);
        Page<CardDto> cardDtos = cards.map(CardDtoMapper::toDto);
        return ResponseEntity.ok(PagedResponse.fromPage(cardDtos));
    }

    // Пользователь: делает запрос на блокировку карты
    @PostMapping("/block/{cardId}")
    @Operation(summary = "Пользователь: Запрос блокировки", description = "Пользователь может только сделать запрос блокировки по ид, а заблокировать может только Администратор")
    public ResponseEntity<CardDto> blockRequestCard(@PathVariable Long cardId) {
        Card card = cardService.blockRequestCardForUser(cardId, userService.getCurrentUserId());
        return ResponseEntity.ok(CardDtoMapper.toDto(card));
    }

    // Пользователь: делает запрос на блокировку карты
    @GetMapping("/balance/{cardId}")
    @Operation(summary = "Пользователь: Баланс", description = "Запрос баланса карты по ид")
    public ResponseEntity<CardBalansDto> balanceCard(@PathVariable Long cardId) {
        Card card = cardService.findByIdForUser(cardId, userService.getCurrentUserId());
        return ResponseEntity.ok(CardDtoMapper.toBalanceDto(card));
    }

    // Администратор: видит все карты
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    @Operation(summary = "Администратор: Все карты", description = "Получение всех карт. Включена постраничная выдача по 10 записей")
    public ResponseEntity<PagedResponse<CardDto>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.findAll(pageable);
        Page<CardDto> cardsDto = cards.map(CardDtoMapper::toDto);
        return ResponseEntity.ok(PagedResponse.fromPage(cardsDto));
    }

    // Администратор: видит все карты для блокировки
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all_block_request")
    @Operation(summary = "Администратор: Все карты с запросом блокировки", description = "Получение всех карт, по которым пользователи отправили запросы блокировки. Включена постраничная выдача по 10 записей")
    public ResponseEntity<PagedResponse<CardDto>> getAllBlockRequestCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Card> cards = cardService.findByStatus(StatusCard.BLOCK_REQUEST, pageable);
        Page<CardDto> cardDtos = cards.map(CardDtoMapper::toDto);
        return ResponseEntity.ok(PagedResponse.fromPage(cardDtos));
    }

    // Администратор: блокирует карту
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/block_admin/{cardId}")
    @Operation(summary = "Администратор: Блокировка", description = "Администратор блокирует конкретную карту по ид")
    public ResponseEntity<CardDto> blockCard(@PathVariable Long cardId) {
        Card card = cardService.blockCardForUser(cardId, userService.getCurrentUserId());
        return ResponseEntity.ok(CardDtoMapper.toDto(card));
    }

    // Администратор: блокирует все карты по запросу пользователя
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/block_request_admin")
    @Operation(summary = "Администратор: Блокировка всех карт по запросу", description = "Администратор блокирует все карты, на которые пользователи отправляли запросы")
    public ResponseEntity<CardDto> blockRequestAllCard() {
        cardService.blockAllRequestCardForUser();
        return ResponseEntity.noContent().build();
    }

    // Администратор: создаёт карту
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    @Operation(summary = "Администратор: Создание карты", description = "Администратор создает карту для пользователя. Номер карты случайный, баланс нулевой. Статус NEW_CARD")
    public ResponseEntity<CardDto> createCard(@RequestBody CardRegistrationDto card) {
        // Получим новую карту для пользователя (её нужно будет активировать)
        Optional<User>  userCard = userRepository.findByUsername(card.getUsername());
        if (userCard.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        Card newCard = CardDtoMapper.regDtoToCard(userCard.get());


        Card saved = cardService.save(newCard);
        return ResponseEntity.ok(CardDtoMapper.toDto(saved));
    }

    // Администратор: создаёт карты по списку
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create_list")
    @Operation(summary = "Администратор: Создание карт по списку пользователей", description = "Администратор создает карты по списку пользователей. Номер карты случайный, баланс нулевой. Статус NEW_CARD")
    public ResponseEntity<List<CardDto>> createListCard(@RequestBody List<CardRegistrationDto> cardList) {

        List<CardDto> savedList = new ArrayList<>();
        for(var card: cardList) {
            // Получим новую карту для пользователя (её нужно будет активировать)
            Optional<User> userCard = userRepository.findByUsername(card.getUsername());
            if (userCard.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            Card newCard = CardDtoMapper.regDtoToCard(userCard.get());
            Card saved = cardService.save(newCard);
            savedList.add(CardDtoMapper.toDto(saved));
        }
        return ResponseEntity.ok(savedList);
    }

    // Администратор: активирует карту
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/activate/{cardId}")
    @Operation(summary = "Администратор: Активация карты", description = "Администратор активирует существующую карту по ид")
    public ResponseEntity<CardDto> activateCard(@PathVariable Long cardId) {
        Card card = cardService.activateCard(cardId);
        return ResponseEntity.ok(CardDtoMapper.toDto(card));
    }

    // Администратор: массовая активация карт по списку ID
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/activate_list")
    @Operation(summary = "Администратор: Активация списка карт", description = "Администратор активирует по списку существующие карты")
    public ResponseEntity<List<CardDto>> activateCards(@RequestBody List<Long> cardIds) {
        List<Card> activatedCards = cardService.activateCards(cardIds);
        List<CardDto> cardDtos = activatedCards.stream()
                .map(CardDtoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cardDtos);
    }

    // Администратор: удаляет карту
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{cardId}")
    @Operation(summary = "Администратор: Удаление карты", description = "Администратор удаляет карту по ид")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

}