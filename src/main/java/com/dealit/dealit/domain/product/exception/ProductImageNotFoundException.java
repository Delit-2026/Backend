package com.dealit.dealit.domain.product.exception;

import org.springframework.http.HttpStatus;

public class ProductImageNotFoundException extends ProductException {

	public ProductImageNotFoundException(String message) {
		super(HttpStatus.BAD_REQUEST, "PRODUCT_IMAGE_NOT_FOUND", message);
	}
}
