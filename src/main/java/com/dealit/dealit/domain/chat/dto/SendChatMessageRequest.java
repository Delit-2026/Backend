package com.dealit.dealit.domain.chat.dto;

import com.dealit.dealit.domain.chat.entity.ChatMessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendChatMessageRequest(
        @NotNull ChatMessageType messageType,
        @NotBlank @Size(min = 1, max = 1000) String content
) {}