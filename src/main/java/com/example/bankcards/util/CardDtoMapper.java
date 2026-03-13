package com.example.bankcards.util;

import com.example.bankcards.dto.CardBalansDto;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.StatusCard;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

public class CardDtoMapper {

    public static CardDto toDto(Card card) {
        if (card == null) return null;
        return CardDto.builder()
                .id(card.getId())
                .number(maskCardNumber(card.getNumber()))
                .expiryDate(card.getExpiryDate())
                .balance(card.getBalance())
                .status(card.getStatus())
                .user(card.getUser().getUsername())
                .build();
    }

    public static CardBalansDto toBalanceDto(Card card) {
        if (card == null) return null;
        return CardBalansDto.builder()
                .id(card.getId())
                .number(maskCardNumber(card.getNumber()))
                .expiryDate(card.getExpiryDate())
                .balance(card.getBalance())
                .build();
    }

    public static Card regDtoToCard(User user){
        return Card.builder()
                //.id(id)
                .number(CardDtoMapper.generateRandomCardNumber())
                .expiryDate(CardDtoMapper.generateExpiryDate())
                .status(StatusCard.NEW_CARD)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build();
    }

    public static String maskCardNumber(String number) {
        if (number == null || number.length() < 4) return number;
        return "**** **** **** " + number.substring(number.length() - 4);
    }

    /**
     * Генерирует случайный 16-значный номер банковской карты.
     * Использует алгоритм Luhn для проверки корректности номера.
     */
    public static String generateRandomCardNumber() {
        Random random = new Random();
        StringBuilder number = new StringBuilder();

        // Генерация первых 15 цифр (например, начинаем с 4 для Visa)
        number.append("4"); // Пример: номера Visa начинаются с 4
        for (int i = 0; i < 14; i++) {
            number.append(random.nextInt(10));
        }

        // Вычисляем последнюю цифру по алгоритму Луна
        int checksum = calculateLuhnChecksum(number.toString());
        number.append(checksum);

        return number.toString();
    }

    /**
     * Вычисляет контрольную цифру по алгоритму Луна.
     */
    private static int calculateLuhnChecksum(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = (digit % 10) + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (sum * 9) % 10;
    }

    /**
     * Возвращает дату окончания срока действия — путь будет 30 дней от текущей даты.
     */
    public static LocalDate generateExpiryDate() {
        return LocalDate.now().plusDays(30);
    }
}