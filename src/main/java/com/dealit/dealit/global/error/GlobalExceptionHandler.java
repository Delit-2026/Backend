package com.dealit.dealit.global.error;

import com.dealit.dealit.domain.auth.exception.InvalidCredentialsException;
import com.dealit.dealit.domain.chat.exception.ChatForbiddenException;
import com.dealit.dealit.domain.chat.exception.ChatMessageNotFoundException;
import com.dealit.dealit.domain.chat.exception.ChatRoomNotFoundException;
import com.dealit.dealit.domain.chat.exception.DuplicateChatReportException;
import com.dealit.dealit.domain.chat.exception.DuplicateChatRoomException;
import com.dealit.dealit.domain.chat.exception.ProductNotFoundException;
import com.dealit.dealit.domain.member.exception.DuplicateMemberException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
		return ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				"INVALID_REQUEST",
				exception.getMessage(),
				List.of()
		);
	}

	@ExceptionHandler(ProductNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleProductNotFound(ProductNotFoundException exception) {
		return ErrorResponse.of(
				HttpStatus.NOT_FOUND.value(),
				"PRODUCT_NOT_FOUND",
				exception.getMessage(),
				List.of()
		);
	}

	@ExceptionHandler(DuplicateMemberException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleDuplicateMemberException(DuplicateMemberException exception) {
		return ErrorResponse.of(HttpStatus.CONFLICT.value(), "DUPLICATE_MEMBER", exception.getMessage(), List.of());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse handleInvalidCredentialsException(InvalidCredentialsException exception) {
		return ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS", exception.getMessage(), List.of());
	}

	private ErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
		return new ErrorResponse.FieldErrorDetail(
				fieldError.getField(),
				fieldError.getRejectedValue(),
				fieldError.getDefaultMessage()
		);
	}

	@ExceptionHandler(ChatRoomNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleChatRoomNotFound(ChatRoomNotFoundException exception) {
		return ErrorResponse.of(
				HttpStatus.NOT_FOUND.value(),
				"CHAT_ROOM_NOT_FOUND",
				exception.getMessage(),
				List.of()
		);
	}

	@ExceptionHandler(ChatMessageNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleChatMessageNotFound(ChatMessageNotFoundException exception) {
		return ErrorResponse.of(
				HttpStatus.NOT_FOUND.value(),
				"CHAT_MESSAGE_NOT_FOUND",
				exception.getMessage(),
				List.of()
		);
	}

	@ExceptionHandler(ChatForbiddenException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorResponse handleChatForbidden(ChatForbiddenException exception) {
		return ErrorResponse.of(
				HttpStatus.FORBIDDEN.value(),
				"CHAT_FORBIDDEN",
				exception.getMessage(),
				List.of()
		);
	}

	@ExceptionHandler(DuplicateChatRoomException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleDuplicateChatRoom(DuplicateChatRoomException exception) {
		return ErrorResponse.of(
				HttpStatus.CONFLICT.value(),
				"DUPLICATE_CHAT_ROOM",
				exception.getMessage(),
				List.of()
		);
	}

	@ExceptionHandler(DuplicateChatReportException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public ErrorResponse handleDuplicateChatReport(DuplicateChatReportException exception) {
		return ErrorResponse.of(
				HttpStatus.CONFLICT.value(),
				"DUPLICATE_CHAT_REPORT",
				exception.getMessage(),
				List.of()
		);
	}
}