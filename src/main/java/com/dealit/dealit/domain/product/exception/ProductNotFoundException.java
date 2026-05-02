package com.dealit.dealit.domain.product.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ProductException {

    public ProductNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", message);
    }
}