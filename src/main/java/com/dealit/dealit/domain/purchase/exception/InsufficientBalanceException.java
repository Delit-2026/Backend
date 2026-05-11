package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends PurchaseException {

	public InsufficientBalanceException() {
		super(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "딜릿머니 잔액이 부족합니다.");
	}
}
