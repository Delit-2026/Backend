package com.dealit.dealit.domain.wallet.exception;

import org.springframework.http.HttpStatus;

public class InsufficientWalletBalanceException extends WalletException {

	public InsufficientWalletBalanceException() {
		super(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "딜릿머니 잔액이 부족합니다.");
	}
}
