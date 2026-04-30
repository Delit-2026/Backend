package com.dealit.dealit.domain.chat.dto;

import java.time.LocalDateTime;

public record ReportChatMessageResponse(
        Long reportId,
        Long messageId,
        String reason,
        LocalDateTime reportedAt
) {}