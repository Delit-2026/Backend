package com.dealit.dealit.domain.notification.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotificationException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public NotificationException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}
}
