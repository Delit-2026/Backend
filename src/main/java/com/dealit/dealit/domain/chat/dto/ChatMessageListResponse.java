package com.dealit.dealit.domain.chat.dto;

import java.util.List;

public record ChatMessageListResponse(
        List<ChatMessageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}