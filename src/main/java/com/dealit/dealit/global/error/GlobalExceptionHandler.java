package com.dealit.dealit.global.error;

import com.dealit.dealit.domain.auction.exception.AuctionException;
import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		List<ErrorResponse.FieldErrorDetail> errors = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toFieldErrorDetail)
			.toList();

		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Request validation failed.", errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadableException(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_REQUEST", "Request body is invalid.", List.of()));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
		List<ErrorResponse.FieldErrorDetail> errors = exception.getConstraintViolations()
			.stream()
			.map(violation -> new ErrorResponse.FieldErrorDetail(
				violation.getPropertyPath().toString(),
				violation.getInvalidValue(),
				violation.getMessage()
			))
			.toList();

		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Request validation failed.", errors));
	}

	@ExceptionHandler(DuplicateMemberException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateMemberException(DuplicateMemberException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "DUPLICATE_MEMBER", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(AuctionException.class)
	public ResponseEntity<ErrorResponse> handleAuctionException(AuctionException exception) {
		return ResponseEntity.status(exception.getStatus())
			.body(ErrorResponse.of(exception.getStatus().value(), exception.getCode(), exception.getMessage(), List.of()));
	}

	private ErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
		return new ErrorResponse.FieldErrorDetail(
			fieldError.getField(),
			fieldError.getRejectedValue(),
			fieldError.getDefaultMessage()
		);
	}
}
