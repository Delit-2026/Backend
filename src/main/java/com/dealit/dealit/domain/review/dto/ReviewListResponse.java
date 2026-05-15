package com.dealit.dealit.domain.review.dto;

import java.util.List;

public record ReviewListResponse(
	ReviewRatingSummaryResponse ratingSummary,
	List<ReviewResponse> content,
	int page,
	int size,
	long totalElements,
	boolean hasNext
) {
}
