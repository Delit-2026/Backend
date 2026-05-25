package com.dealit.dealit.domain.category.dto;

import java.util.List;

public record AiCategoryRecommendationResponse(
	Long recommendedCategoryId,
	Double confidence,
	String reason,
	List<AiCategoryRecommendationAlternativeResponse> alternatives,
	String modelVersion
) {
}
