package com.dealit.dealit.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 인증된 회원 정보")
public record CurrentMemberResponse(
	@Schema(description = "회원 ID", example = "1")
	Long memberId,

	@Schema(description = "로그인 아이디", example = "delit_user")
	String loginId,

	@Schema(description = "이메일", example = "hong@example.com")
	String email,

	@Schema(description = "닉네임", example = "당근한입")
	String nickname
) {
}
