package com.dealit.dealit.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "회원가입 응답")
public record SignUpResponse(
	@Schema(description = "회원 ID", example = "1")
	Long memberId,

	@Schema(description = "로그인 아이디", example = "dealit-user")
	String loginId,

	@Schema(description = "이메일", example = "user@dealit.com")
	String email,

	@Schema(description = "자동 생성 닉네임", example = "Dealit#1")
	String nickname,

	@Schema(description = "생성 시각")
	LocalDateTime createdAt
) {
}
