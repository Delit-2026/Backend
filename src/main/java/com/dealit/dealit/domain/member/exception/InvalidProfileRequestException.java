package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidProfileRequestException extends MemberException {

	public InvalidProfileRequestException(String message) {
		super("INVALID_PROFILE_REQUEST", message, HttpStatus.BAD_REQUEST);
	}
}
