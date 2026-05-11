package com.dealit.dealit.domain.chat.dto;

import com.dealit.dealit.domain.chat.entity.ChatType;
import com.dealit.dealit.domain.purchase.entity.PurchaseStatus;

public record ChatRoomDetailResponse(
        Long roomId,
        Long purchaseId,
        ChatType chatType,
        String currentUserRole,
        PurchaseStatus tradeStatus,
        ProductInfo product,
        OpponentInfo opponent,
        ActionButton actionButton
) {
    public record ProductInfo(
            Long productId,
            String name,
            String thumbnailUrl
    ) {
    }

    public record OpponentInfo(
            Long userId,
            String nickname
    ) {
    }

    public record ActionButton(
            String label,
            boolean enabled,
            String actionType,
            String disabledReason
    ) {
    }
}
