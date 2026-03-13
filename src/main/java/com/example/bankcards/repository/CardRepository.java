package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.enums.StatusCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserId(Long userId);
    Page<Card> findByUserId(Long userId, Pageable pageable);
    List<Card> findByStatus(StatusCard status);
    List<Card> findByUserUsername(String username);
    Optional<Card> findByIdAndUserId(Long id, Long userId);
    Page<Card> findByStatusIn(List<StatusCard> statusList, Pageable pageable);
    Page<Card> findByUserIdAndNumberContaining(Long userId, String number, Pageable pageable);
    @Modifying
    @Query("UPDATE Card c SET c.status = :newStatus WHERE c.status IN :statusList")
    void updateByStatusIn(@Param("statusList")List<StatusCard> listStatus,
                          @Param("newStatus")StatusCard Status);


}