package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record MarkChatRoomAsReadResponse(
        Long roomId,
        int unreadCount,
        LocalDateTime readAt
) {}