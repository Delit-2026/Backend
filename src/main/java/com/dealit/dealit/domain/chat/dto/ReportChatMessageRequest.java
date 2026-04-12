package com.dealit.dealit.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportChatMessageRequest(
        @NotBlank @Size(min = 1, max = 255) String reason
) {}