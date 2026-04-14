package com.dealit.dealit.domain.chat.repository;

import com.dealit.dealit.domain.chat.entity.ChatMessage;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByMessageIdAndDeletedAtIsNull(Long messageId);

    @Query("""
        SELECT m
        FROM ChatMessage m
        WHERE m.roomId = :roomId
          AND m.deletedAt IS NULL
        ORDER BY m.sentAt DESC
    """)
    Page<ChatMessage> findByRoomId(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.roomId = :roomId
          AND m.isRead = false
          AND m.senderId <> :currentUserId
          AND m.deletedAt IS NULL
    """)
    long countUnreadByRoomId(
            @Param("roomId") Long roomId,
            @Param("currentUserId") Long currentUserId
    );

    @Query("""
        SELECT COUNT(m)
        FROM ChatMessage m
        WHERE m.roomId IN (
            SELECT r.roomId
            FROM ChatRoom r
            WHERE (r.sellerId = :userId OR r.buyerId = :userId)
              AND r.deletedAt IS NULL
        )
          AND m.isRead = false
          AND m.senderId <> :userId
          AND m.deletedAt IS NULL
    """)
    long countTotalUnreadForUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ChatMessage m
        SET m.isRead = true
        WHERE m.roomId = :roomId
          AND m.isRead = false
          AND m.senderId <> :currentUserId
          AND m.deletedAt IS NULL
    """)
    int markAllAsRead(
            @Param("roomId") Long roomId,
            @Param("currentUserId") Long currentUserId
    );

    @Query("""
        SELECT m
        FROM ChatMessage m
        WHERE m.roomId = :roomId
          AND m.deletedAt IS NULL
        ORDER BY m.sentAt DESC
    """)
    Page<ChatMessage> findLatestMessagePage(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    default Optional<ChatMessage> findLatestMessage(Long roomId) {
        Page<ChatMessage> page = findLatestMessagePage(roomId, Pageable.ofSize(1));
        if (page.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(page.getContent().get(0));
    }
}