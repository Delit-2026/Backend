package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatUnreadCountUpdatedEvent(
        String type,
        int totalUnreadCount,
        LocalDateTime emittedAt
) {
    public static ChatUnreadCountUpdatedEvent of(int totalUnreadCount, LocalDateTime emittedAt) {
        return new ChatUnreadCountUpdatedEvent("chat.unread-count.updated", totalUnreadCount, emittedAt);
    }
}
