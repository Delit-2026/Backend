package com.dealit.dealit.domain.review.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReviewResponse(
	Long reviewId,
	Long reviewerId,
	String reviewerNickname,
	Long revieweeId,
	String revieweeNickname,
	Long productId,
	String productName,
	Long auctionId,
	BigDecimal rating,
	String content,
	OffsetDateTime createdAt
) {
}
