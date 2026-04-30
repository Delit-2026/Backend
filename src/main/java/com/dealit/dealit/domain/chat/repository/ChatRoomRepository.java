package com.dealit.dealit.domain.chat.repository;

import com.dealit.dealit.domain.chat.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findBySellerIdAndBuyerIdAndProductIdAndDeletedAtIsNull(
            Long sellerId,
            Long buyerId,
            Long productId
    );

    @Query("""
        SELECT r
        FROM ChatRoom r
        WHERE (r.sellerId = :userId OR r.buyerId = :userId)
          AND r.deletedAt IS NULL
        ORDER BY r.updatedAt DESC
    """)
    Page<ChatRoom> findAllByParticipant(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
        SELECT r
        FROM ChatRoom r
        WHERE r.roomId = :roomId
          AND (r.sellerId = :userId OR r.buyerId = :userId)
          AND r.deletedAt IS NULL
    """)
    Optional<ChatRoom> findAccessibleRoom(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId
    );
}