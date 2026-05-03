package com.dealit.dealit.domain.product.dto;

public record DeleteProductResponse(
	Long productId,
	boolean deleted
) {
}
