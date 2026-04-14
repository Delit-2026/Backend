package com.dealit.dealit.domain.chat.dto;

import jakarta.validation.constraints.NotNull;

public record CreateChatRoomRequest(
        @NotNull Long productId,
        @NotNull Long receiverId
) {}