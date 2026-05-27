package com.dealit.dealit.domain.price.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PriceRecommendationException extends RuntimeException {

	private final String code;
	private final HttpStatus status;

	public PriceRecommendationException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}
}
