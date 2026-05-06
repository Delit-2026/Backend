package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class PurchaseNotCompletableException extends PurchaseException {

	public PurchaseNotCompletableException() {
		super(HttpStatus.CONFLICT, "PURCHASE_NOT_COMPLETABLE", "완료 처리할 수 없는 구매 내역입니다.");
	}
}
