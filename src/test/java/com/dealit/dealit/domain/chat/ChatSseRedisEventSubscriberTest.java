package com.dealit.dealit.domain.chat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.domain.chat.service.ChatSseRedisEvent;
import com.dealit.dealit.domain.chat.service.ChatSseRedisEventPublisher;
import com.dealit.dealit.domain.chat.service.ChatSseRedisEventSubscriber;
import com.dealit.dealit.domain.chat.service.ChatSseService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

class ChatSseRedisEventSubscriberTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatSseRedisEventPublisher publisher = mock(ChatSseRedisEventPublisher.class);
    private final ChatSseService chatSseService = mock(ChatSseService.class);
    private final ChatSseRedisEventSubscriber subscriber = new ChatSseRedisEventSubscriber(
            objectMapper,
            publisher,
            chatSseService
    );

    @Test
    void redisEventFromOtherServerIsForwardedToLocalEmitter() throws Exception {
        when(publisher.getServerId()).thenReturn("server-1");
        JsonNode payload = objectMapper.valueToTree(Map.of("type", "chat.room.updated"));
        ChatSseRedisEvent event = new ChatSseRedisEvent("server-2", 1L, "chat.room.updated", payload);

        subscriber.onMessage(redisMessage(event), null);

        verify(chatSseService).publishRemote(eq(1L), eq("chat.room.updated"), eq(payload));
    }

    @Test
    void redisEventFromSameServerIsIgnored() throws Exception {
        when(publisher.getServerId()).thenReturn("server-1");
        JsonNode payload = objectMapper.valueToTree(Map.of("type", "chat.room.updated"));
        ChatSseRedisEvent event = new ChatSseRedisEvent("server-1", 1L, "chat.room.updated", payload);

        subscriber.onMessage(redisMessage(event), null);

        verify(chatSseService, never()).publishRemote(eq(1L), eq("chat.room.updated"), eq(payload));
    }

    private Message redisMessage(ChatSseRedisEvent event) throws Exception {
        byte[] body = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
        byte[] channel = "dealit:chat:sse-events".getBytes(StandardCharsets.UTF_8);

        return new Message() {
            @Override
            public byte[] getBody() {
                return body;
            }

            @Override
            public byte[] getChannel() {
                return channel;
            }
        };
    }
}
