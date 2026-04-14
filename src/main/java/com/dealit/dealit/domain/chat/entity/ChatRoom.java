package com.dealit.dealit.domain.chat.entity;

import com.dealit.dealit.global.entity.BaseEntity; // TODO: 실제 BaseEntity 경로로 수정
import jakarta.persistence.*;
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
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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