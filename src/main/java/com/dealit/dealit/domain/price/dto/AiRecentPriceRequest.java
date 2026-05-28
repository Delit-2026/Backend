package com.dealit.dealit.domain.price.dto;

public record AiRecentPriceRequest(
	Long price,
	String title,
	String soldAt
) {
}
