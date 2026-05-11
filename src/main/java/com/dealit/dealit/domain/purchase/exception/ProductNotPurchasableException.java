package com.dealit.dealit.domain.purchase.exception;

import org.springframework.http.HttpStatus;

public class ProductNotPurchasableException extends PurchaseException {

	public ProductNotPurchasableException() {
		this("현재 구매할 수 없는 상품입니다.");
	}

	public ProductNotPurchasableException(String message) {
		super(HttpStatus.CONFLICT, "PRODUCT_NOT_PURCHASABLE", message);
	}
}
