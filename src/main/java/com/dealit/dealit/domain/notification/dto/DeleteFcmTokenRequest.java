package com.dealit.dealit.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteFcmTokenRequest(
	@NotBlank(message = "FCM 토큰은 필수입니다.")
	@Size(max = 4096, message = "FCM 토큰은 4096자 이하이어야 합니다.")
	String token
) {
}
