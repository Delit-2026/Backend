package com.dealit.dealit.domain.chat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dealit.dealit.global.event.service.EventStreamRedisEvent;
import com.dealit.dealit.global.event.service.EventStreamRedisEventPublisher;
import com.dealit.dealit.global.event.service.EventStreamRedisEventSubscriber;
import com.dealit.dealit.global.event.service.EventStreamService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;

class EventStreamRedisEventSubscriberTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventStreamRedisEventPublisher publisher = mock(EventStreamRedisEventPublisher.class);
    private final EventStreamService eventStreamService = mock(EventStreamService.class);
    private final EventStreamRedisEventSubscriber subscriber = new EventStreamRedisEventSubscriber(
            objectMapper,
            publisher,
            eventStreamService
    );

    @Test
    void redisEventFromOtherServerIsForwardedToLocalEmitter() throws Exception {
        when(publisher.getServerId()).thenReturn("server-1");
        JsonNode payload = objectMapper.valueToTree(Map.of("type", "chat.room.updated"));
        EventStreamRedisEvent event = new EventStreamRedisEvent("server-2", 1L, "chat.room.updated", payload);

        subscriber.onMessage(redisMessage(event), null);

        verify(eventStreamService).publishRemote(eq(1L), eq("chat.room.updated"), eq(payload));
    }

    @Test
    void redisEventFromSameServerIsIgnored() throws Exception {
        when(publisher.getServerId()).thenReturn("server-1");
        JsonNode payload = objectMapper.valueToTree(Map.of("type", "chat.room.updated"));
        EventStreamRedisEvent event = new EventStreamRedisEvent("server-1", 1L, "chat.room.updated", payload);

        subscriber.onMessage(redisMessage(event), null);

        verify(eventStreamService, never()).publishRemote(eq(1L), eq("chat.room.updated"), eq(payload));
    }

    private Message redisMessage(EventStreamRedisEvent event) throws Exception {
        byte[] body = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
        byte[] channel = "dealit:events:sse-events".getBytes(StandardCharsets.UTF_8);

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
