package com.dealit.dealit.domain.chat.entity;

import com.dealit.dealit.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_rooms",
        indexes = {
                @Index(name = "idx_chat_room_seller", columnList = "seller_id"),
                @Index(name = "idx_chat_room_buyer", columnList = "buyer_id"),
                @Index(name = "idx_chat_room_product", columnList = "product_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(
        name = "chat_room_seq_generator",
        sequenceName = "chat_room_seq",
        allocationSize = 1
)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_room_seq_generator")
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_type", nullable = false, length = 20)
    private ChatType chatType;

    public static ChatRoom create(Long sellerId, Long buyerId, Long productId, ChatType chatType) {
        ChatRoom room = new ChatRoom();
        room.sellerId = sellerId;
        room.buyerId = buyerId;
        room.productId = productId;
        room.chatType = chatType;
        return room;
    }

    public boolean isParticipant(Long userId) {
        return sellerId.equals(userId) || buyerId.equals(userId);
    }

    public Long getOpponentId(Long currentUserId) {
        return sellerId.equals(currentUserId) ? buyerId : sellerId;
    }
}