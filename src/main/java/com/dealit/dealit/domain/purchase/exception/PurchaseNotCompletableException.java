package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class PurchaseNotCompletableException extends PurchaseException {

	public PurchaseNotCompletableException() {
		this("완료 처리할 수 없는 구매 내역입니다.");
	}

	public PurchaseNotCompletableException(String message) {
		super(HttpStatus.CONFLICT, "PURCHASE_NOT_COMPLETABLE", message);
	}
}
