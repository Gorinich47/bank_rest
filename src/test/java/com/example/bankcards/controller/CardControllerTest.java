package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.Role;
import com.example.bankcards.enums.StatusCard;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.*;
import com.example.bankcards.util.CardDtoMapper;
import com.example.bankcards.util.ChecksData;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.example.bankcards.controller.CardController.class)
@Import(SecurityConfig.class)
public class CardControllerTest {

    private static final String ROLE_USER="USER";
    private static final String ROLE_ADMIN="ADMIN";

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private CardRepository cardRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private com.example.bankcards.service.CheckService checkDto;

    @MockitoBean // Создает мок и кладет его в контекст теста
    private  com.example.bankcards.service.JwtService jwtService;

    @MockitoBean(name = "userService") // Создает мок и кладет его в контекст теста
    private com.example.bankcards.service.UserService userService;

    private final MockMvc mockMvc;

    @Autowired
    CardControllerTest(MockMvc mockMvc){
        this.mockMvc = mockMvc;
    }

    private Card card;
    private CardDto cardDto;
    private CardBalansDto balanceDto;
    private Page<Card> cardPage;
    private Page<CardDto> cardDtoPage;
    private PagedResponse<CardDto> pagedResponse;


    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .firstName("Иван")
                .lastName("Петров")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .firstName("Петр")
                .lastName("Петров")
                .email("user1@example.com")
                .role(Role.USER)
                .build();

        User user2 = User.builder()
                .id(1L)
                .username("user2")
                .firstName("Иван")
                .lastName("Петров")
                .email("user2@example.com")
                .role(Role.USER)
                .build();


        card = Card.builder()
                .id(1L)
                .number("1000200030004000")
                .user(user)
                .expiryDate(LocalDate.parse("2026-05-31"))
                .status(StatusCard.NEW_CARD)
                .balance(BigDecimal.valueOf(1000.0))
                .build();

        cardDto = CardDto.builder()
                .id(1L)
                .number("1000200030004000")
                .expiryDate(LocalDate.parse("2026-05-31"))
                .balance(BigDecimal.valueOf(1000.0))
                .status(StatusCard.ACTIVE)
                .user("user")
                .build();

        balanceDto = CardBalansDto.builder()
                .id(1L)
                .number("1000200030004000")
                .expiryDate(LocalDate.parse("2026-05-31"))
                .balance(BigDecimal.valueOf(1000.0))
                .build();

        cardPage = new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1);

        cardDtoPage = new PageImpl<>(List.of(CardDtoMapper.toDto(card)), PageRequest.of(0, 10), 1);

        pagedResponse = PagedResponse.fromPage(cardPage.map(CardDtoMapper::toDto));
        //pagedDtoResponse = PagedResponse.fromPage(cardDtoPage.map(CardDtoMapper::toDto));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void getMyCards_ValidRequest_ReturnsPagedResponse() throws Exception {

        when(cardService.findByUserIdAndNumberContaining(null,  PageRequest.of(0, 10)))
                .thenReturn(cardDtoPage);

        mockMvc.perform(get("/api/cards/my")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].balance").value(1000.0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void getMyCards_WithSearchNumber_ReturnsFilteredCards() throws Exception {

        when(cardService.findByUserIdAndNumberContaining("1234",  PageRequest.of(0, 10)))
                .thenReturn(cardDtoPage);

        mockMvc.perform(get("/api/cards/my")
                        .param("page", "0")
                        .param("size", "10")
                        .param("number", "1234")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].number").value("**** **** **** 4000"));
    }

    @Test
    void getMyCards_UnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cards/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void blockRequestCard_ValidId_ReturnsUpdatedCard() throws Exception {
        cardDto.setStatus(StatusCard.BLOCK_REQUEST);
        when(cardService.blockRequestCardForUser(1L)).thenReturn(cardDto);

        mockMvc.perform(post("/api/cards/block/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCK_REQUEST"));

        verify(cardService, times(1)).blockRequestCardForUser(1L);
    }

    @Test
    void blockRequestCard_UnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/cards/block/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_USER)
    void balanceCard_ValidId_ReturnsBalance() throws Exception {

        when(cardService.findByIdForUser(1L)).thenReturn(balanceDto);

        mockMvc.perform(get("/api/cards/balance/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.0));

        verify(cardService, times(1)).findByIdForUser(1L);
    }

    @Test
    void balanceCard_UnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cards/balance/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void getAllCards_ReturnsPagedResponse() throws Exception {

        when(cardService.findAllDto(PageRequest.of(0, 10))).thenReturn(cardDtoPage);

        mockMvc.perform(get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllCards_UnauthorizedUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cards/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void getAllBlockRequestCards_ReturnsOnlyBlockRequests() throws Exception {

        cardDtoPage.forEach(cardDto -> cardDto.setStatus(StatusCard.BLOCK_REQUEST));

        when(cardService.findByStatus(StatusCard.BLOCK_REQUEST, PageRequest.of(0, 10)))
                .thenReturn(cardDtoPage);

        mockMvc.perform(get("/api/cards/all_block_request")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("BLOCK_REQUEST"));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void blockCard_ValidId_ReturnsBlockedCard() throws Exception {

        when(cardService.blockCardForUser(1L)).thenReturn(cardDto);

        mockMvc.perform(post("/api/cards/block_admin/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void blockRequestAllCard_Success_ReturnsNoContent() throws Exception {
        doNothing().when(cardService).blockAllRequestCardForUser();

        mockMvc.perform(post("/api/cards/block_request_admin")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).blockAllRequestCardForUser();
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void createCard_ValidData_ReturnsCreatedCard() throws Exception {

        when(cardService.createCard(any(CardRegistrationDto.class))).thenReturn(cardDto);

        mockMvc.perform(post("/api/cards/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(cardService, times(1)).createCard(any(CardRegistrationDto.class));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void createCard_UserNotFound_ReturnsBadRequest() throws Exception {

        when(cardService.createCard(any(CardRegistrationDto.class)))
                 .thenThrow(new ResourceNotFoundException("Пользователь с именем 'unknown' не существует"));

        mockMvc.perform(post("/api/cards/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "unknown"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с именем 'unknown' не существует"));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void createListCard_AllValid_ReturnsListOfCards() throws Exception {

        // Создаем объекты, которые хотим получить на выходе
        CardDto cardDto1 = CardDto.builder().id(1L).user("user1").build();
        CardDto cardDto2 = CardDto.builder().id(2L).user("user2").build();

        // Настраиваем мок: первый вызов вернет card1, второй — card2
        when(cardService.createCard(any(CardRegistrationDto.class)))
                .thenReturn(cardDto1, cardDto2);

        mockMvc.perform(post("/api/cards/create_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {"username": "user1"},
                                    {"username": "user2"}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void createListCard_OneUserNotFound_ReturnsNotFound() throws Exception {

        // Создаем объекты, которые хотим получить на выходе
        CardDto cardDto1 = CardDto.builder().id(1L).user("user1").build();
        CardDto cardDto2 = CardDto.builder().id(2L).user("user2").build();

        // Настраиваем мок: первый вызов вернет card1, второй — card2
        when(cardService.createCard(any(CardRegistrationDto.class)))
            //.thenReturn(cardDto1, cardDto2);
            .thenThrow(new ResourceNotFoundException("Пользователь с именем 'unknown' не существует"));

        mockMvc.perform(post("/api/cards/create_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {"username": "unknown"},
                                    {"username": "user1"}
                                ]
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Пользователь с именем 'unknown' не существует"));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void activateCard_ValidId_ReturnsActivatedCard() throws Exception {

        when(cardService.activateCard(1L)).thenReturn(cardDto);

        mockMvc.perform(post("/api/cards/activate/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void activateCards_ValidList_ReturnsActivatedCards() throws Exception {
        when(cardService.activateCards(List.of(1L, 2L))).thenReturn(List.of(cardDto, cardDto));

        mockMvc.perform(post("/api/cards/activate_list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = ROLE_ADMIN)
    void deleteCard_ValidId_ReturnsNoContent() throws Exception {
        doNothing().when(cardService).deleteCard(1L);

        mockMvc.perform(delete("/api/cards/delete/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).deleteCard(1L);
    }
}
