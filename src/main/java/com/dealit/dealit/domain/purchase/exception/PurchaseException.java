package com.dealit.dealit.domain.purchase.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class PurchaseException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	protected PurchaseException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}
}
