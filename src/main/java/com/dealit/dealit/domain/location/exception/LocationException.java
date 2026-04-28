package com.dealit.dealit.domain.location.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class LocationException extends RuntimeException {

	private final String code;
	private final HttpStatus status;

	public LocationException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}
}
