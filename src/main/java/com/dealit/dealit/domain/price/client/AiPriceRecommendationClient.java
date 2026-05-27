package com.dealit.dealit.domain.price.client;

import com.dealit.dealit.domain.category.client.AiCategoryRecommendationProperties;
import com.dealit.dealit.domain.price.dto.AiPriceRecommendationRequest;
import com.dealit.dealit.domain.price.dto.AiPriceRecommendationResponse;
import com.dealit.dealit.domain.price.exception.PriceRecommendationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AiPriceRecommendationClient {

	private final RestClient restClient;

	public AiPriceRecommendationClient(
		AiCategoryRecommendationProperties properties,
		RestClient.Builder restClientBuilder
	) {
		this.restClient = restClientBuilder
			.baseUrl(properties.normalizedBaseUrl())
			.build();
	}

	public AiPriceRecommendationResponse recommend(AiPriceRecommendationRequest request) {
		try {
			AiPriceRecommendationResponse response = restClient
				.post()
				.uri("/api/v1/recommendations/prices")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(AiPriceRecommendationResponse.class);

			if (response == null) {
				throw new PriceRecommendationException(
					"PRICE_RECOMMENDATION_EMPTY_RESPONSE",
					"AI 가격 추천 응답이 비어 있습니다.",
					HttpStatus.BAD_GATEWAY
				);
			}

			return response;
		} catch (RestClientResponseException exception) {
			throw new PriceRecommendationException(
				"PRICE_RECOMMENDATION_FAILED",
				"AI 가격 추천 호출에 실패했습니다. status=%d, body=%s"
					.formatted(exception.getStatusCode().value(), exception.getResponseBodyAsString()),
				HttpStatus.BAD_GATEWAY
			);
		} catch (RestClientException exception) {
			throw new PriceRecommendationException(
				"PRICE_RECOMMENDATION_FAILED",
				"AI 가격 추천 호출에 실패했습니다.",
				HttpStatus.BAD_GATEWAY
			);
		}
	}
}
