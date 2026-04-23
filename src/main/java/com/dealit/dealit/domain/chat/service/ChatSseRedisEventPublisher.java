package com.dealit.dealit.domain.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.sse.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatSseRedisEventPublisher {

    @Getter
    private final String serverId = UUID.randomUUID().toString();

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.chat.sse.redis.channel}")
    private String channel;

    public void publish(Long userId, String eventName, Object payload) {
        ChatSseRedisEvent event = new ChatSseRedisEvent(
                serverId,
                userId,
                eventName,
                objectMapper.valueToTree(payload)
        );

        try {
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException | DataAccessException exception) {
            log.warn("Failed to publish chat SSE event to Redis. eventName={}, userId={}", eventName, userId, exception);
        }
    }
}
