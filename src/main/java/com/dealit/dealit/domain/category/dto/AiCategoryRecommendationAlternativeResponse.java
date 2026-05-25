package com.dealit.dealit.domain.category.dto;

public record AiCategoryRecommendationAlternativeResponse(
	Long categoryId,
	Double confidence
) {
}
