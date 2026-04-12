package com.dealit.dealit.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
	@Schema(description = "Access Token")
	String accessToken,

	@Schema(description = "토큰 타입", example = "Bearer")
	String tokenType,

	@Schema(description = "만료 시간(ms)", example = "3600000")
	long expiresIn
) {
}
