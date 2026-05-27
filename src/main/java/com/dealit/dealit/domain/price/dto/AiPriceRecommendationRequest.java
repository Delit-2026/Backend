package com.dealit.dealit.domain.price.dto;

import java.util.List;

public record AiPriceRecommendationRequest(
	String title,
	String description,
	Long categoryId,
	String categoryName,
	String saleType,
	List<String> imageUrls,
	List<AiRecentPriceRequest> recentPrices
) {
	public static AiPriceRecommendationRequest of(String title, String description, String saleType) {
		return new AiPriceRecommendationRequest(
			title,
			description,
			null,
			null,
			saleType,
			List.of(),
			List.of()
		);
	}
}
