package com.example.bankcards.service;

import com.example.bankcards.dto.CardBalansDto;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardRegistrationDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.exception.AlreadyExistsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardDtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class CardService {

    private final CardRepository cardRepository;
    private final CheckService checkService;
    private final UserService userService;
    @Autowired
    public CardService(CardRepository cardRepository, CheckService checkService,UserService userService) {
        this.cardRepository = cardRepository;
        this.checkService = checkService;
        this.userService = userService;

    }

    public Page<CardDto> findAllDto(Pageable pageable) {
        Page<Card> cards = cardRepository.findAll(pageable);
        Page<CardDto> cardsDto = cards.map(CardDtoMapper::toDto);
        return cardsDto;
    }

    public Page<CardDto> findByStatus(StatusCard statusCard, Pageable pageable) {
        //String statusName = StatusCard.BLOCK_REQUEST.name();
        Page<Card> cards = cardRepository.findByStatusIn(List.of(statusCard), pageable);
        Page<CardDto> cardsDto = cards.map(CardDtoMapper::toDto);

        return cardsDto;
    }


    public Page<CardDto> findByUserIdAndNumberContaining(String number, Pageable pageable) {

        Page<Card> cards;

        Long userId = userService.getCurrentUserId();
        if (number == null || number.isEmpty()) {
            cards= cardRepository.findByUserId(userId, pageable);
        } else {
            cards = cardRepository.findByUserIdAndNumberContaining(userId, number, pageable);
        }

        return cards.map(CardDtoMapper::toDto);
    }

    @Transactional
    public CardDto blockRequestCardForUser(Long cardId) {

        Long userId = userService.getCurrentUserId();

        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена или не принадлежит пользователю"));

        if (card.getStatus()==StatusCard.BLOCKED) {
            throw new AlreadyExistsException("Карта уже заблокирована");
        }

        if (card.getStatus()==StatusCard.BLOCK_REQUEST) {
            throw new AlreadyExistsException("Запрос на блокировку карты уже отправлен ранее");
        }

        card.setStatus(StatusCard.BLOCK_REQUEST);

        Card blockCard = save(card);

        return CardDtoMapper.toDto(blockCard);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void blockAllRequestCardForUser() {
        cardRepository.updateByStatusIn(List.of(StatusCard.BLOCK_REQUEST), StatusCard.BLOCKED);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CardDto blockCardForUser(Long cardId) {

        Long userId= userService.getCurrentUserId();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена"));

        if (card.getStatus() == StatusCard.BLOCKED) {
            throw new AlreadyExistsException("Карта уже заблокирована");
        }

        card.setStatus(StatusCard.BLOCKED);
        card = save(card);

        CardDto cardsDto = CardDtoMapper.toDto(card);

        return cardsDto;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CardDto activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена"));

        if (card.getStatus() == StatusCard.ACTIVE) {
            throw new AlreadyExistsException(String.format("Карта id=%d № %s не может быть активирована, т.к. она уже активная.",cardId, CardDtoMapper.maskCardNumber(card.getNumber())));
        }

        if (card.getStatus() == StatusCard.EXPIRED) {
            throw new AlreadyExistsException(String.format("Карта id=%d № %s не может быть активирована, т.к. истек срок действия.",cardId, CardDtoMapper.maskCardNumber(card.getNumber())));
        }

        card.setStatus(StatusCard.ACTIVE);

        card = save(card);

        CardDto CardDto = CardDtoMapper.toDto(card);

        return CardDto;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<CardDto> activateCards(List<Long> cardIds) {
        return cardIds.stream().map(id -> {
            Card card = cardRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Карта не найдена: %d", id)));

            if (card.getStatus() == StatusCard.ACTIVE) {
                throw new AlreadyExistsException(String.format("Карта id=%d № %s не может быть активирована, т.к. она уже активная.", id, CardDtoMapper.maskCardNumber(card.getNumber())));
            }

            if (card.getStatus() == StatusCard.EXPIRED) {
                throw new AlreadyExistsException(String.format("Карта id=%d № %s не может быть активирована, т.к. истек срок действия.", id, CardDtoMapper.maskCardNumber(card.getNumber())));
            }

            card.setStatus(StatusCard.ACTIVE);
            card = save(card);

            CardDto cardDto =  CardDtoMapper.toDto(card);

            return cardDto;


        }).collect(Collectors.toList());
    }

    public CardDto createCard(CardRegistrationDto card){

        checkService.checkFields(card);
        // Получим новую карту для пользователя (её нужно будет активировать)
        User userCard = userService.findByUsername(card.getUsername());
        Card newCard = CardDtoMapper.regDtoToCard(userCard);

        Card saved = save(newCard);

        CardDto cardDtoSaved = CardDtoMapper.toDto(saved);

        return cardDtoSaved;
    }

    public Card save(Card card) {
        return cardRepository.save(card);
    }

    public void deleteCard(Long cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new IllegalArgumentException("Карта не найдена");
        }
        cardRepository.deleteById(cardId);
    }

    public CardBalansDto findByIdForUser(Long cardId){

        Long userId = userService.getCurrentUserId();
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена или не принадлежит пользователю"));

        return CardDtoMapper.toBalanceDto(card);
    }

    public Optional<Card> findById(Long cardId) {
        return cardRepository.findById(cardId);
    }
}