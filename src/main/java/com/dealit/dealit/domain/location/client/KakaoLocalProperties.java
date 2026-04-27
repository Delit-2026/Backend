package com.dealit.dealit.domain.location.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.local")
public record KakaoLocalProperties(
	String baseUrl,
	String restApiKey
) {

	public String normalizedBaseUrl() {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("kakao.local.base-url must not be blank");
		}

		return baseUrl.endsWith("/")
			? baseUrl.substring(0, baseUrl.length() - 1)
			: baseUrl;
	}

	public String authorizationHeaderValue() {
		if (restApiKey == null || restApiKey.isBlank()) {
			throw new IllegalStateException("kakao.local.rest-api-key must not be blank");
		}

		return "KakaoAK " + restApiKey;
	}
}
