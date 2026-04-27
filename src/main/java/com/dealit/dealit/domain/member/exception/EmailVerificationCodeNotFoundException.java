package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationCodeNotFoundException extends MemberException {

	public EmailVerificationCodeNotFoundException(String message) {
		super("EMAIL_VERIFICATION_CODE_NOT_FOUND", message, HttpStatus.BAD_REQUEST);
	}
}
