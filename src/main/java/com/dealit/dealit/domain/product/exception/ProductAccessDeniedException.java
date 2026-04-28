package com.dealit.dealit.domain.product.exception;

import org.springframework.http.HttpStatus;

public class ProductAccessDeniedException extends ProductException {

	public ProductAccessDeniedException(String message) {
		super(HttpStatus.FORBIDDEN, "PRODUCT_ACCESS_DENIED", message);
	}
}
