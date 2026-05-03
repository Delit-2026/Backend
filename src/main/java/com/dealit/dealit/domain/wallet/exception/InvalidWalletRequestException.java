package com.dealit.dealit.domain.wallet.exception;

import org.springframework.http.HttpStatus;

public class InvalidWalletRequestException extends WalletException {

	public InvalidWalletRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "INVALID_WALLET_REQUEST", message);
	}
}
