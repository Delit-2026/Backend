package com.dealit.dealit.domain.product.dto;

import com.dealit.dealit.domain.product.ProductSaleType;
import com.dealit.dealit.domain.product.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalesManagementProductResponse(
	Long productId,
	String name,
	String description,
	ProductSaleType saleType,
	ProductStatus status,
	BigDecimal price,
	String location,
	Long categoryId,
	String categoryName,
	String thumbnailImageUrl,
	boolean editable,
	boolean deletable,
	LocalDateTime createdAt
) {
}
