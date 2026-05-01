package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record RoomUnreadCountResponse(
        Long roomId,
        int unreadCount,
        LocalDateTime updatedAt
) {}
