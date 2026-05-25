package com.dealit.dealit.domain.category.dto;

public record CategoryRecommendationAlternative(
	Long categoryId,
	String categoryName,
	Double confidence
) {
}
