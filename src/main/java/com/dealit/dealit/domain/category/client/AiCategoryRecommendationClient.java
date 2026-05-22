package com.dealit.dealit.domain.category.client;

import com.dealit.dealit.domain.category.dto.AiCategoryRecommendationRequest;
import com.dealit.dealit.domain.category.dto.AiCategoryRecommendationResponse;
import com.dealit.dealit.domain.category.exception.CategoryRecommendationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class AiCategoryRecommendationClient {

	private final AiCategoryRecommendationProperties properties;
	private final RestClient.Builder restClientBuilder;

	public AiCategoryRecommendationResponse recommend(AiCategoryRecommendationRequest request) {
		try {
			AiCategoryRecommendationResponse response = restClientBuilder
				.baseUrl(properties.normalizedBaseUrl())
				.build()
				.post()
				.uri("/api/v1/recommendations/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(AiCategoryRecommendationResponse.class);

			if (response == null) {
				throw new CategoryRecommendationException(
					"CATEGORY_RECOMMENDATION_EMPTY_RESPONSE",
					"AI 카테고리 추천 응답이 비어 있습니다.",
					HttpStatus.BAD_GATEWAY
				);
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw new CategoryRecommendationException(
				"CATEGORY_RECOMMENDATION_FAILED",
				"AI 카테고리 추천 호출에 실패했습니다. status=%d, body=%s"
					.formatted(exception.getStatusCode().value(), exception.getResponseBodyAsString()),
				HttpStatus.BAD_GATEWAY
			);
		} catch (RestClientException exception) {
			throw new CategoryRecommendationException(
				"CATEGORY_RECOMMENDATION_FAILED",
				"AI 카테고리 추천 호출에 실패했습니다.",
				HttpStatus.BAD_GATEWAY
			);
		}
	}
}
