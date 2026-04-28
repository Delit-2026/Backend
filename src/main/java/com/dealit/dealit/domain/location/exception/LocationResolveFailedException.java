package com.dealit.dealit.domain.location.exception;

import org.springframework.http.HttpStatus;

public class LocationResolveFailedException extends LocationException {

	public LocationResolveFailedException(String message) {
		super("LOCATION_RESOLVE_FAILED", message, HttpStatus.BAD_GATEWAY);
	}
}
