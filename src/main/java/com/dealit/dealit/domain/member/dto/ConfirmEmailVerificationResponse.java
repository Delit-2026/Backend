package com.dealit.dealit.domain.member.dto;

public record ConfirmEmailVerificationResponse(
	String email,
	boolean verified
) {
}
