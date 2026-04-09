package com.dealit.dealit.global.security;

public record AuthenticatedMember(
	Long memberId,
	String loginId,
	String role
) {
}
