package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.repository.CardRepository;
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

    @Autowired
    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Page<Card> findAll(Pageable pageable) {
        return cardRepository.findAll(pageable);
    }

    public Page<Card> findByStatus(StatusCard statusCard, Pageable pageable) {
        //String statusName = StatusCard.BLOCK_REQUEST.name();
        return cardRepository.findByStatusIn(List.of(statusCard), pageable);
    }

    public Page<Card> findByUserIdAndNumberContaining(Long userId, String number, Pageable pageable) {
        if (number == null || number.isEmpty()) {
            return cardRepository.findByUserId(userId, pageable);
        }
        return cardRepository.findByUserIdAndNumberContaining(userId, number, pageable);
    }

    @Transactional
    public Card blockRequestCardForUser(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена или не принадлежит пользователю"));

        if (card.getStatus()==StatusCard.BLOCKED) {
            throw new IllegalArgumentException("Карта уже заблокирована");
        }

        if (card.getStatus()==StatusCard.BLOCK_REQUEST) {
            throw new IllegalArgumentException("Запрос на блокировку карты уже отправлен");
        }

        card.setStatus(StatusCard.BLOCK_REQUEST);

        Card blockCard = cardRepository.save(card);

        return blockCard;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void blockAllRequestCardForUser() {
        cardRepository.updateByStatusIn(List.of(StatusCard.BLOCK_REQUEST), StatusCard.BLOCKED);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Card blockCardForUser(Long cardId, Long userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена"));

        if (card.getStatus() == StatusCard.BLOCKED) {
            throw new IllegalArgumentException("Карта уже заблокирована");
        }

        card.setStatus(StatusCard.BLOCKED);
        return cardRepository.save(card);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Card activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена"));

        if (card.getStatus() == StatusCard.ACTIVE) {
            throw new IllegalArgumentException("Карта уже активна");
        }

        card.setStatus(StatusCard.ACTIVE);
        return cardRepository.save(card);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Card> activateCards(List<Long> cardIds) {
        return cardIds.stream().map(id -> {
            Card card = cardRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Карта не найдена: " + id));

            if (card.getStatus() != StatusCard.NEW_CARD) {
                throw new IllegalArgumentException("Карта не может быть активирована (статус не NEW_CARD): " + id);
            }

            card.setStatus(StatusCard.ACTIVE);
            return cardRepository.save(card);
        }).collect(Collectors.toList());
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

    public Card findByIdForUser(Long cardId, Long userId){
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Карта не найдена или не принадлежит пользователю"));
    }

    public Optional<Card> findById(Long cardId) {
        return cardRepository.findById(cardId);
    }
}