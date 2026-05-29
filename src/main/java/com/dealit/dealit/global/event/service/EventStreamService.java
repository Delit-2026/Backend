package com.dealit.dealit.global.event.service;

import com.dealit.dealit.domain.chat.dto.ChatRoomUpdatedEvent;
import com.dealit.dealit.domain.chat.dto.ChatUnreadCountUpdatedEvent;
import com.dealit.dealit.global.event.dto.EventStreamConnectedEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EventStreamService {

    private static final long DEFAULT_TIMEOUT_MS = 30L * 60L * 1000L;

    private final EventStreamEmitterRepository emitterRepository;
    private final Optional<EventStreamRedisEventPublisher> redisEventPublisher;
    private final Executor eventStreamTaskExecutor;

    public EventStreamService(
            EventStreamEmitterRepository emitterRepository,
            Optional<EventStreamRedisEventPublisher> redisEventPublisher,
            @Qualifier("eventStreamTaskExecutor") Executor eventStreamTaskExecutor
    ) {
        this.emitterRepository = emitterRepository;
        this.redisEventPublisher = redisEventPublisher;
        this.eventStreamTaskExecutor = eventStreamTaskExecutor;
    }

    public SseEmitter subscribe(Long userId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emitterRepository.save(userId, emitterId, emitter);

        emitter.onCompletion(() -> emitterRepository.remove(userId, emitterId));
        emitter.onTimeout(() -> emitterRepository.remove(userId, emitterId));
        emitter.onError(exception -> emitterRepository.remove(userId, emitterId));

        send(
                userId,
                emitterId,
                emitter,
                "event.stream.connected",
                EventStreamConnectedEvent.of(userId, LocalDateTime.now())
        );
        return emitter;
    }

    public void publishRoomUpdated(Long userId, ChatRoomUpdatedEvent event) {
        publishToUser(userId, "chat.room.updated", event);
    }

    public void publishUnreadCountUpdated(Long userId, ChatUnreadCountUpdatedEvent event) {
        publishToUser(userId, "chat.unread-count.updated", event);
    }

    public void publishRemote(Long userId, String eventName, Object payload) {
        sendToUser(userId, eventName, payload);
    }

    public void publish(Long userId, String eventName, Object payload) {
        publishToUser(userId, eventName, payload);
    }

    @Scheduled(fixedDelayString = "${app.events.sse.heartbeat-interval-ms:25000}")
    public void sendHeartbeat() {
        for (Map.Entry<Long, Map<String, SseEmitter>> userEntry : emitterRepository.findAll().entrySet()) {
            Long userId = userEntry.getKey();
            for (Map.Entry<String, SseEmitter> emitterEntry : userEntry.getValue().entrySet()) {
                String emitterId = emitterEntry.getKey();
                SseEmitter emitter = emitterEntry.getValue();
                eventStreamTaskExecutor.execute(() -> sendHeartbeat(userId, emitterId, emitter));
            }
        }
    }

    private void sendHeartbeat(Long userId, String emitterId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
        } catch (IOException | RuntimeException exception) {
            emitterRepository.remove(userId, emitterId);
        }
    }

    private void publishToUser(Long userId, String eventName, Object payload) {
        sendToUser(userId, eventName, payload);
        redisEventPublisher.ifPresent(publisher -> publisher.publish(userId, eventName, payload));
    }

    private void sendToUser(Long userId, String eventName, Object payload) {
        for (Map.Entry<String, SseEmitter> entry : emitterRepository.findAllByUserId(userId).entrySet()) {
            send(userId, entry.getKey(), entry.getValue(), eventName, payload);
        }
    }

    private void send(Long userId, String emitterId, SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name(eventName)
                    .data(payload));
        } catch (IOException | RuntimeException exception) {
            emitterRepository.remove(userId, emitterId);
        }
    }
}
