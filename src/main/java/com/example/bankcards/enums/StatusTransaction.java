package com.example.bankcards.enums;

import lombok.Getter;

@Getter
public enum StatusTransaction {
    SUCCESSFUL("Успешно"),
    CANCELED("Отменено"),
    WAITING("В ожидании"),
    ERROR("Ошибка");

    private final String title;

    StatusTransaction(String title) {
        this.title = title;
    }
}