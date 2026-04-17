package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record DeleteChatMessageResponse(
        String message,
        Long messageId,
        LocalDateTime timestamp
) {}