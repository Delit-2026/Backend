package com.dealit.dealit.global.event.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class EventStreamEmitterRepository {

    private final Map<Long, Map<String, SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter save(Long userId, String emitterId, SseEmitter emitter) {
        emittersByUser
                .computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .put(emitterId, emitter);
        return emitter;
    }

    public Map<String, SseEmitter> findAllByUserId(Long userId) {
        return emittersByUser.getOrDefault(userId, Map.of());
    }

    public void remove(Long userId, String emitterId) {
        Map<String, SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitterId);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
