package com.example.bankcards.util;

import java.lang.reflect.Field;
import java.util.Collection;

@FunctionalInterface
public interface FieldChecker {
    void check(Object dto);

    // Статический метод-реализация по умолчанию
    static FieldChecker defaultChecker() {
        return dto -> {
            if (dto == null) throw new IllegalArgumentException("DTO не может быть null");

            Class<?> clazz = dto.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                // Проверяем наличие нашей аннотации
                if (field.isAnnotationPresent(CheckField.class)) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(dto);
                        CheckField annotation = field.getAnnotation(CheckField.class);

                        // Проверка на null
                        if (value == null) {
                            throw new IllegalArgumentException(
                                    "Поле '" + field.getName() + "': должно присутствовать обязательно"
                            );
                        }
                        // Проверка на пустую строку/коллекцию
                        if(isBlank(value)) {
                            throw new IllegalArgumentException(
                                    "Поле '" + field.getName() + "': " + annotation.message()
                            );
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Ошибка доступа к полю", e);
                    }
                }
            }
        };
    }

    private static boolean isBlank(Object value) {
        if (value instanceof String s) return s.trim().isEmpty();
        if (value instanceof Collection<?> c) return c.isEmpty();
        return false;
    }
}