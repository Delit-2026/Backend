package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatSseConnectedEvent(
        String type,
        Long userId,
        LocalDateTime connectedAt
) {
    public static ChatSseConnectedEvent of(Long userId, LocalDateTime connectedAt) {
        return new ChatSseConnectedEvent("chat.stream.connected", userId, connectedAt);
    }
}
