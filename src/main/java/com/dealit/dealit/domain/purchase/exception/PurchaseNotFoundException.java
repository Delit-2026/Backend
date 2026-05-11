package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class PurchaseNotFoundException extends PurchaseException {

	public PurchaseNotFoundException() {
		super(HttpStatus.NOT_FOUND, "PURCHASE_NOT_FOUND", "구매 내역을 찾을 수 없습니다.");
	}
}
