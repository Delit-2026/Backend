package com.dealit.dealit.global.error;

import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
		List<ErrorResponse.FieldErrorDetail> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toFieldErrorDetail)
			.toList();

		return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "입력값을 확인해주세요.", errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleNotReadableException(HttpMessageNotReadableException exception) {
		return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST", "요청 본문을 확인해주세요.", List.of());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleConstraintViolationException(ConstraintViolationException exception) {
		return ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "입력값을 확인해주세요.", List.of());
	}

	@ExceptionHandler(DuplicateMemberException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleDuplicateMemberException(DuplicateMemberException exception) {
		return ErrorResponse.of(HttpStatus.CONFLICT.value(), "DUPLICATE_MEMBER", exception.getMessage(), List.of());
	}

	private ErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
		return new ErrorResponse.FieldErrorDetail(
			fieldError.getField(),
			fieldError.getRejectedValue(),
			fieldError.getDefaultMessage()
		);
	}
}
