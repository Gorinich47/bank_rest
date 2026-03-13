package com.example.bankcards.controller;

import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferDto;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@RestController
@Validated
@RequestMapping("/api/transactions")
@Tag(name = "Контроллер транзакций", description = "Переводы между картами пользователя, просмотр транзакций")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    @Autowired
    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    // Пользователь: делает перевод между своими картами
    @PostMapping("/transfer")
    @Operation(summary = "Перевод между картами", description = "Пользователь может перевести деньги с одной карты на другую")
    public ResponseEntity<TransactionDto> transferBetweenCards(@RequestBody TransferDto transferDto) {
        String description = transferDto.getDescription();

        TransactionDto transaction = transactionService.transferBetweenUserCards(
                userService.getCurrentUserId(),
                transferDto.getSenderCardId(),
                transferDto.getReceiverCardId(),
                transferDto.getAmount(),
                description==null ? "" :  description);

        return ResponseEntity.ok(transaction);
    }

    // Администратор: видит все транзакции за период
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Все транзакции", description = "Администратор получает список всех операций за указанный промежуток времени. Включена постраничная выдача 10 записей")
    public ResponseEntity<PagedResponse<TransactionDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam @NotNull(message = "Дата начала обязательна") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @NotNull(message = "Дата окончания обязательна") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionDto> transactions = transactionService.findByDateBetween(start, end, pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(transactions));
    }
}