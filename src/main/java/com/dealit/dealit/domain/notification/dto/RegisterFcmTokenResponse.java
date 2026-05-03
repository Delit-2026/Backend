package com.dealit.dealit.domain.notification.dto;

public record RegisterFcmTokenResponse(
	Long fcmTokenId,
	boolean registered
) {
}
