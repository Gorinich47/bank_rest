package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.TransactionDto;
import com.example.bankcards.dto.TransferDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.enums.StatusTransaction;
import com.example.bankcards.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfig.class)
@WebMvcTest(com.example.bankcards.controller.TransactionController.class)
public class TransactionControllerTest {

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private com.example.bankcards.service.CheckService checkDto;

    @MockitoBean // Создает мок и кладет его в контекст теста
    private  com.example.bankcards.service.JwtService jwtService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    private User user;
    private CardDto card1;
    private CardDto card2;
    private TransferDto transferDto;
    private TransactionDto transactionDto;
    private PagedResponse<TransactionDto> pagedResponse;

    private final MockMvc mockMvc;

    @Autowired
    TransactionControllerTest(MockMvc mockMvc){
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("User1")
                .firstName("Иван")
                .lastName("Петров")
                .email("User1@example.com")
                .role(Role.USER)
                .build();

        card1 = CardDto.builder()
                .id(1L)
                .number("10002000300004000")
                .expiryDate(LocalDate.parse(("2026-05-31")))
                .balance(BigDecimal.valueOf(1000))
                .status(StatusCard.ACTIVE)
                .user("User1")
                .build();

        card2 = CardDto.builder()
                .id(2L)
                .number("20002000300004000")
                .expiryDate(LocalDate.parse(("2026-05-31")))
                .balance(BigDecimal.valueOf(2000))
                .status(StatusCard.ACTIVE)
                .user("User1")
                .build();

        transferDto = new TransferDto().builder()
                .amount(BigDecimal.valueOf(100))
                .senderCardId(1L)
                .receiverCardId(2L)
                .status(StatusTransaction.SUCCESSFUL)
                .description("Перевод между картами")
                .build();

        transactionDto = TransactionDto.builder()
                .id(1L)
                .transactionDate(LocalDateTime.now())
                .amount(BigDecimal.valueOf(100))
                .status(StatusTransaction.SUCCESSFUL)
                .senderCard(null)
                .receiverCard(null)
                .description("Перевод между картами")
                .build();

        var page = new org.springframework.data.domain.PageImpl<>(
                List.of(transactionDto),
                org.springframework.data.domain.PageRequest.of(0, 10),
                1
        );
        pagedResponse = PagedResponse.fromPage(page);
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_ValidData_ReturnsTransactionDto() throws Exception {


        when(transactionService.transferBetweenUserCards(any(TransferDto.class)))
                .thenReturn(transactionDto);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 100.0,
                                    "senderCardId": 1,
                                    "receiverCardId": 2,
                                    "description": "Перевод между картами"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.description").value("Перевод между картами"));

        verify(transactionService, times(1)).transferBetweenUserCards(any(TransferDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_MissingDescription_UsesEmptyString() throws Exception {

        when(transactionService.transferBetweenUserCards(any(TransferDto.class)))
                .thenReturn(transactionDto);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "senderCardId": 1,
                                    "receiverCardId": 2,
                                    "amount": 100.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100.0));

        verify(transactionService, times(1)).transferBetweenUserCards(
                any(TransferDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_MissingDescription_ReturnsUnprocessableContent_Null_senderCardId() throws Exception {

        doThrow(new IllegalArgumentException("senderCardId: ID карты не может быть пустым"))
                .when(transactionService).transferBetweenUserCards(any(TransferDto.class));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "receiverCardId": 2,
                                    "amount": 100.0
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$[0].status").value(422))
                .andExpect(jsonPath("$[0].message").value("senderCardId: ID карты не может быть пустым"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void transferBetweenCards_MissingDescription_ReturnsUnprocessableContent_Empty_SenderCardId() throws Exception {

        doThrow(new IllegalArgumentException("senderCardId: ID карты не может быть пустым"))
                .when(transactionService).transferBetweenUserCards(any(TransferDto.class));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "senderCardId": "",
                                    "receiverCardId": 2,
                                    "amount": 100.0
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$[0].status").value(422))
                .andExpect(jsonPath("$[0].message").value("senderCardId: ID карты не может быть пустым"));
    }

    @Test
    void transferBetweenCards_UnauthorizedUser_ReturnsForbidden() throws Exception {

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "senderCardId": 1,
                                    "receiverCardId": 2,
                                    "amount": 100.0
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllTransactions_ValidPeriod_ReturnsPagedResponse() throws Exception {

        List<TransactionDto> transactions = Collections.singletonList(transactionDto);
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionDto> pagedResponse = new PageImpl<>(transactions, pageable, 1);

        when(transactionService.findByDateBetween(any(), any(), any()))
                .thenReturn(pagedResponse);

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 12, 31, 23, 59, 59);

        mockMvc.perform(get("/api/transactions")
                        .param("page", "0")
                        .param("size", "10")
                        .param("start", "2025-01-01T00:00:00")
                        .param("end", "2025-12-31T23:59:59")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(transactionService, times(1)).findByDateBetween(
                eq(start), eq(end), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllTransactions_MissingParams_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTransactions_UnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .param("start", "2025-01-01T00:00:00")
                        .param("end", "2025-12-31T23:59:59")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}