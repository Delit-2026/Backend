package com.dealit.dealit.global.security.jwt;

// JWT payload에서 애플리케이션이 실제로 사용하는 최소 사용자 정보
public record JwtClaims(
	Long memberId,
	String loginId,
	String role
) {
}
