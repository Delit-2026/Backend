package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class PurchaseForbiddenException extends PurchaseException {

	public PurchaseForbiddenException() {
		super(HttpStatus.FORBIDDEN, "PURCHASE_FORBIDDEN", "구매 내역을 조회할 권한이 없습니다.");
	}
}
