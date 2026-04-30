package com.dealit.dealit.global.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(EventStreamRedisEventPublisher.class)
public class EventStreamRedisEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final EventStreamRedisEventPublisher publisher;
    private final EventStreamService eventStreamService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            EventStreamRedisEvent event = objectMapper.readValue(body, EventStreamRedisEvent.class);
            if (publisher.getServerId().equals(event.originServerId())) {
                return;
            }
            eventStreamService.publishRemote(event.userId(), event.eventName(), event.payload());
        } catch (JsonProcessingException exception) {
            log.warn("Failed to consume SSE event from Redis.", exception);
        }
    }
}
