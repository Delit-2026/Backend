package com.dealit.dealit.domain.chat.dto;

import java.util.List;

public record ChatRoomListResponse(
        List<ChatRoomListItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {}