package com.dealit.dealit.domain.product.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ProductException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	protected ProductException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}
}
