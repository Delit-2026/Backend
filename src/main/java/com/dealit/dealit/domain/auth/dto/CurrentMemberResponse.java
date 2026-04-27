package com.dealit.dealit.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 인증된 회원 정보")
public record CurrentMemberResponse(
	@Schema(description = "회원 ID", example = "1")
	Long memberId,

	@Schema(description = "로그인 아이디", example = "dealit_user")
	String loginId,

	@Schema(description = "이메일", example = "hong@example.com", nullable = true)
	String email,

	@Schema(description = "닉네임", example = "딜잇유저")
	String nickname,

	@Schema(description = "이메일 인증 여부", example = "false")
	boolean verified
) {
}
