package com.dealit.dealit.global.event.controller;

import com.dealit.dealit.global.event.service.EventStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Events", description = "앱 전역 실시간 이벤트 API")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventStreamController {

    private final EventStreamService eventStreamService;

    @Operation(summary = "앱 전역 SSE 구독")
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return eventStreamService.subscribe(currentUserId);
    }
}
