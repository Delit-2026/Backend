package com.dealit.dealit.domain.product.exception;

import org.springframework.http.HttpStatus;

public class InvalidProductRequestException extends ProductException {

	public InvalidProductRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_REQUEST", message);
	}
}
