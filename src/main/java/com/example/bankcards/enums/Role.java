package com.example.bankcards.enums;

import lombok.*;

@Getter
public enum Role {
    ADMIN("Администратор"),
    USER("Пользователь"),
    GUEST("Гость");

    private final String title;

    Role(String title) {
        this.title = title;
    }
}