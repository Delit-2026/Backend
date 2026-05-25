package com.dealit.dealit.domain.category.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.category-recommendation")
public record AiCategoryRecommendationProperties(
	String baseUrl
) {

	public String normalizedBaseUrl() {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("app.ai.category-recommendation.base-url must not be blank");
		}

		return baseUrl.endsWith("/")
			? baseUrl.substring(0, baseUrl.length() - 1)
			: baseUrl;
	}
}
