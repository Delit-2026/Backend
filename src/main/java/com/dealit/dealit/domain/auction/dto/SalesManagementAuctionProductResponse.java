package com.dealit.dealit.domain.auction.dto;

import com.dealit.dealit.domain.auction.AuctionStatus;
import com.dealit.dealit.domain.auction.ProductSaleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record SalesManagementAuctionProductResponse(
	Long productId,
	String name,
	String description,
	ProductSaleType saleType,
	AuctionStatus status,
	BigDecimal price,
	BigDecimal startPrice,
	String location,
	Long categoryId,
	String categoryName,
	String thumbnailImageUrl,
	int bidCount,
	boolean editable,
	boolean deletable,
	OffsetDateTime auctionEndAt,
	LocalDateTime createdAt
) {
}
