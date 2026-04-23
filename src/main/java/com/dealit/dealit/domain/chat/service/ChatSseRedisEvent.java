package com.dealit.dealit.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;

public record ChatSseRedisEvent(
        String originServerId,
        Long userId,
        String eventName,
        JsonNode payload
) {
}
