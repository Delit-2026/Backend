package com.dealit.dealit.domain.chat.dto;

import com.dealit.dealit.domain.chat.entity.ChatType;
import java.time.LocalDateTime;

public record ChatRoomListItemResponse(
        Long roomId,
        OpponentInfo opponent,
        ProductInfo product,
        ChatType chatType,
        LastMessageInfo lastMessage,
        int unreadCount,
        LocalDateTime updatedAt
) {
    public record OpponentInfo(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {}

    public record ProductInfo(
            Long productId,
            String name,
            String thumbnailUrl
    ) {}

    public record LastMessageInfo(
            Long messageId,
            String type,
            String content,
            LocalDateTime sentAt
    ) {}
}