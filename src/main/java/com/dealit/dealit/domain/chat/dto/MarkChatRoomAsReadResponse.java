package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record MarkChatRoomAsReadResponse(
        Long roomId,
        String message,
        int unreadCount,
        LocalDateTime readAt
) {}