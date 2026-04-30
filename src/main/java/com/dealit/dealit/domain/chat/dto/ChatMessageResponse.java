package com.dealit.dealit.domain.chat.dto;

import com.dealit.dealit.domain.chat.entity.ChatMessageType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long messageId,
        Long senderId,
        String senderNickname,
        ChatMessageType messageType,
        String content,
        boolean isRead,
        LocalDateTime sentAt
) {}