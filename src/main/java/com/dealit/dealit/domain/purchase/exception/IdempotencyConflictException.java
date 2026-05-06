package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends PurchaseException {

	public IdempotencyConflictException() {
		super(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "같은 멱등성 키로 다른 상품을 구매할 수 없습니다.");
	}
}
