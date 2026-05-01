package com.dealit.dealit.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterFcmTokenRequest(
	@NotBlank(message = "FCM 토큰은 필수입니다.")
	@Size(max = 4096, message = "FCM 토큰은 4096자 이하이어야 합니다.")
	String token,

	@Size(max = 100, message = "기기 ID는 100자 이하이어야 합니다.")
	String deviceId,

	@Size(max = 30, message = "플랫폼은 30자 이하이어야 합니다.")
	String platform
) {
}
