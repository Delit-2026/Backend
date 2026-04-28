package com.dealit.dealit.domain.member.dto;

public record SendEmailVerificationResponse(
	String email,
	long expiresInSeconds
) {
}
