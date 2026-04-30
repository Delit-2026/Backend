package com.dealit.dealit.global.event.service;

import com.fasterxml.jackson.databind.JsonNode;

public record EventStreamRedisEvent(
        String originServerId,
        Long userId,
        String eventName,
        JsonNode payload
) {
}
