package com.dealit.dealit.domain.category.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CategoryRecommendationException extends RuntimeException {

	private final String code;
	private final HttpStatus status;

	public CategoryRecommendationException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}
}
