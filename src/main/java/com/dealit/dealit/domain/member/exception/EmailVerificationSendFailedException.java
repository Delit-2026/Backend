package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationSendFailedException extends MemberException {

	public EmailVerificationSendFailedException(String message) {
		super("EMAIL_VERIFICATION_SEND_FAILED", message, HttpStatus.BAD_GATEWAY);
	}
}
