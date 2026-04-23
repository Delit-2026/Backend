package com.dealit.dealit.domain.chat.service;

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
@ConditionalOnBean(ChatSseRedisEventPublisher.class)
public class ChatSseRedisEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ChatSseRedisEventPublisher publisher;
    private final ChatSseService chatSseService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            ChatSseRedisEvent event = objectMapper.readValue(body, ChatSseRedisEvent.class);
            if (publisher.getServerId().equals(event.originServerId())) {
                return;
            }
            chatSseService.publishRemote(event.userId(), event.eventName(), event.payload());
        } catch (JsonProcessingException exception) {
            log.warn("Failed to consume chat SSE event from Redis.", exception);
        }
    }
}
