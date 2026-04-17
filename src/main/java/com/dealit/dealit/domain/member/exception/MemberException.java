package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class MemberException extends RuntimeException {

	private final String code;
	private final HttpStatus status;

	public MemberException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
