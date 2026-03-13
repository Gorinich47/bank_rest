package com.example.bankcards.enums;

import lombok.*;

@Getter
public enum StatusCard {
    NEW_CARD ("Новая картка"),
    ACTIVE("Активна"),
    BLOCK_REQUEST("Запрос на блокировку"),
    BLOCKED("Заблокирована"),
    EXPIRED("Истек срок");

    private final String title;

    StatusCard(String title) {
        this.title = title;
    }
}