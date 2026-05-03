package com.dealit.dealit.domain.wallet.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WalletException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public WalletException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}
}
