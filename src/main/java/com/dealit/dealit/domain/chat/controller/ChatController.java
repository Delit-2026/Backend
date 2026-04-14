package com.dealit.dealit.domain.chat.controller;

import com.dealit.dealit.domain.chat.dto.*;
import com.dealit.dealit.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chats", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 생성")
    @PostMapping("/rooms")
    public ResponseEntity<CreateChatRoomResponse> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(chatService.createChatRoom(request, currentUserId));
    }

    @Operation(summary = "채팅방 목록 조회")
    @GetMapping("/rooms")
    public ResponseEntity<ChatRoomListResponse> getChatRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity.ok(chatService.getChatRooms(currentUserId, page, size));
    }

    @Operation(summary = "채팅 메시지 목록 조회")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageListResponse> getChatMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity.ok(chatService.getChatMessages(roomId, currentUserId, page, size));
    }

    @Operation(summary = "채팅 메시지 전송")
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<SendChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody SendChatMessageRequest request,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(chatService.sendMessage(roomId, request, currentUserId));
    }

    @Operation(summary = "채팅방 읽음 처리")
    @PatchMapping("/rooms/{roomId}/read")
    public ResponseEntity<MarkChatRoomAsReadResponse> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity.ok(chatService.markAsRead(roomId, currentUserId));
    }

    @Operation(summary = "전체 안읽음 개수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity.ok(chatService.getUnreadCount(currentUserId));
    }

    @Operation(summary = "채팅 메시지 신고")
    @PostMapping("/messages/{messageId}/reports")
    public ResponseEntity<ReportChatMessageResponse> reportMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody ReportChatMessageRequest request,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(chatService.reportMessage(messageId, request, currentUserId));
    }

    @Operation(summary = "채팅 메시지 삭제")
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal(expression = "memberId") Long currentUserId
    ) {
        chatService.deleteMessage(messageId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}