package com.dealit.dealit.global.security;

// SecurityContext에 저장되는 인증 사용자 정보
public record AuthenticatedMember(
	Long memberId,
	String loginId,
	String role
) {
}
