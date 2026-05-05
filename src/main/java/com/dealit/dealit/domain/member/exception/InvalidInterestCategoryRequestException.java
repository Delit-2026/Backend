package com.dealit.dealit.domain.member.exception;

import org.springframework.http.HttpStatus;

public class InvalidInterestCategoryRequestException extends MemberException {

	public InvalidInterestCategoryRequestException(String message) {
		super("INVALID_INTEREST_CATEGORY_REQUEST", message, HttpStatus.BAD_REQUEST);
	}
}
