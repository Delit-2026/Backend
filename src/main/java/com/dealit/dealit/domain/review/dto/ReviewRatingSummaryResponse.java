package com.dealit.dealit.domain.review.dto;

public record ReviewRatingSummaryResponse(
	Long memberId,
	double averageRating,
	long reviewCount
) {
}
