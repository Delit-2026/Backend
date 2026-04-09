package com.dealit.dealit.global.security.jwt;

public record JwtClaims(
	Long memberId,
	String loginId,
	String role
) {
}
