package com.dealit.dealit.global.event.dto;

import java.time.LocalDateTime;

public record EventStreamConnectedEvent(
        String type,
        Long userId,
        LocalDateTime connectedAt
) {
    public static EventStreamConnectedEvent of(Long userId, LocalDateTime connectedAt) {
        return new EventStreamConnectedEvent("event.stream.connected", userId, connectedAt);
    }
}
