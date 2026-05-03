package com.dealit.dealit.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SendTestNotificationRequest(
	@NotBlank(message = "알림 제목은 필수입니다.")
	@Size(max = 100, message = "알림 제목은 100자 이하이어야 합니다.")
	String title,

	@NotBlank(message = "알림 내용은 필수입니다.")
	@Size(max = 500, message = "알림 내용은 500자 이하이어야 합니다.")
	String body,

	Map<String, String> data
) {
}
