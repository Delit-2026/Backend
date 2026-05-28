package com.dealit.dealit.domain.price.dto;

public record AiPriceRecommendationResponse(
	Long suggestedPriceMin,
	Long suggestedPrice,
	Long suggestedPriceMax,
	Double confidence,
	String reason,
	java.util.List<String> factors,
	String modelVersion
) {
}
