package com.example.bankcards.entity;

import com.example.bankcards.enums.StatusTransaction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime transactionDate;

    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount; //сумма перевода.

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusTransaction status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_card_id", nullable = false)
    private Card senderCard; //карта отправителя (Card).

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_card_id", nullable = false)
    private Card receiverCard; //карта получателя (Card).

    @Column(name = "description", length = 255)
    private String description; //описание транзакции.

}