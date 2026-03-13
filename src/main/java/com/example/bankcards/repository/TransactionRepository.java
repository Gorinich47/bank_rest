package com.example.bankcards.repository;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.enums.StatusTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySenderCardId(Long senderCardId, Pageable pageable);
    Page<Transaction> findByReceiverCardId(Long receiverCardId, Pageable pageable);
    Page<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<Transaction> findByStatus(StatusTransaction status, Pageable pageable);
    Page<Transaction> findBySenderCardUserId(Long userId, Pageable pageable);
}