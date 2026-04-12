package com.dealit.dealit.global.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
	OffsetDateTime timestamp,
	int status,
	String code,
	String message,
	List<FieldErrorDetail> errors
) {

	public static ErrorResponse of(int status, String code, String message, List<FieldErrorDetail> errors) {
		return new ErrorResponse(OffsetDateTime.now(), status, code, message, errors);
	}

	public record FieldErrorDetail(
		String field,
		Object rejectedValue,
		String reason
	) {
	}
}
