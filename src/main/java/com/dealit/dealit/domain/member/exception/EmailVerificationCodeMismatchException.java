package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class EmailVerificationCodeMismatchException extends MemberException {

	public EmailVerificationCodeMismatchException(String message) {
		super("EMAIL_VERIFICATION_CODE_MISMATCH", message, HttpStatus.BAD_REQUEST);
	}
}
