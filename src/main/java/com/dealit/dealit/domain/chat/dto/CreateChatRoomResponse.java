package com.dealit.dealit.domain.chat.dto;

import com.dealit.dealit.domain.chat.entity.ChatType;
import java.time.LocalDateTime;
import java.util.List;

public record CreateChatRoomResponse(
        Long roomId,
        ChatType chatType,
        ProductInfo product,
        List<ParticipantInfo> participants,
        Boolean isWinner,
        ActionButtons actionButtons,
        LocalDateTime createdAt
) {
    public record ProductInfo(
            Long productId,
            String name,
            String thumbnailUrl,
            String saleType,
            String status
    ) {}

    public record ParticipantInfo(
            Long userId,
            String nickname,
            String role
    ) {}

    public record ActionButtons(
            Boolean canPay,
            Boolean canCompleteTrade,
            String payButtonType,
            String completeTradeButtonType
    ) {}
}