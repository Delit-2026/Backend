package com.dealit.dealit.domain.chat.dto;

import com.dealit.dealit.domain.chat.entity.ChatMessageType;
import java.time.LocalDateTime;

public record SendChatMessageResponse(
        Long messageId,
        Long roomId,
        Long senderId,
        ChatMessageType messageType,
        String content,
        boolean isRead,
        LocalDateTime sentAt
) {}