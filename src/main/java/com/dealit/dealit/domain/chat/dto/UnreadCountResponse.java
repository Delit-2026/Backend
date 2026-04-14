package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record UnreadCountResponse(
        int totalUnreadCount,
        LocalDateTime updatedAt
) {}