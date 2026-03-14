package com.example.bankcards.dto;

import jakarta.validation.Valid;
import lombok.*;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDtoList {
    @Valid // <--- Заставляет валидатор зайти внутрь каждого элемента
    private List<RegistrationDto> dtoList;
}
