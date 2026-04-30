package com.dealit.dealit.domain.chat.controller;

import com.dealit.dealit.domain.chat.dto.SendChatMessageRequest;
import com.dealit.dealit.domain.chat.dto.SendChatMessageResponse;
import com.dealit.dealit.domain.chat.service.ChatService;
import com.dealit.dealit.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chats/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Valid SendChatMessageRequest request,
            Principal principal
    ) {
        Long currentUserId = resolveCurrentUserId(principal);
        SendChatMessageResponse response = chatService.sendMessage(roomId, request, currentUserId);

        messagingTemplate.convertAndSend("/topic/chats/rooms/" + roomId, response);
    }

    private Long resolveCurrentUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new InsufficientAuthenticationException("인증 사용자 정보가 없습니다.");
        }
        Object p = authentication.getPrincipal();
        if (!(p instanceof AuthenticatedMember m)) {
            throw new InsufficientAuthenticationException("인증 사용자 정보가 없습니다.");
        }
        return m.memberId();
    }
}