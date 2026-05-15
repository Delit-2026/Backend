package com.dealit.dealit.domain.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateReviewRequest(
	Long productId,
	Long auctionId,
	@NotNull(message = "rating is required.")
	@DecimalMin(value = "1.0", message = "rating must be at least 1.0.")
	@DecimalMax(value = "5.0", message = "rating must be at most 5.0.")
	@Digits(integer = 1, fraction = 1, message = "rating must have at most one decimal place.")
	BigDecimal rating,
	@NotBlank(message = "content is required.")
	@Size(max = 1000, message = "content must be 1000 characters or less.")
	String content
) {
}
