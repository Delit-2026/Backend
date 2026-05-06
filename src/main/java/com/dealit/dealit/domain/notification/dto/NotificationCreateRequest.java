package com.dealit.dealit.domain.notification.dto;

import com.dealit.dealit.domain.notification.entity.InAppNotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationCreateRequest(
	@NotNull
	InAppNotificationType type,

	@NotBlank
	@Size(max = 100)
	String title,

	@NotBlank
	@Size(max = 500)
	String content,

	@Size(max = 30)
	String targetType,

	Long targetId,

	@Size(max = 500)
	String targetUrl
) {
}
