package com.app.taskmanagement.card.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Catches exceptions thrown anywhere in the application and converts them into
 * clean, consistent JSON error responses.
 *
 * Without this, Spring would return ugly HTML error pages for APIs.
 *
 * @RestControllerAdvice applies this handler globally to all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** 404 — board, list, or member not found in DB */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/** 409 — e.g. trying to add a member who is already on the board */
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	/** 400 — invalid input not caught by @Valid annotation */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/**
	 * 403 — authenticated but not authorized. Example: OBSERVER trying to create a
	 * card, MEMBER trying to delete a board.
	 */
	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	/**
	 * 400 — handles @Valid failures on request body fields. Returns map of {
	 * fieldName → errorMessage } so frontend knows which field failed.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		for (FieldError err : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(err.getField(), err.getDefaultMessage());
		}
		return ResponseEntity.badRequest()
				.body(new ErrorResponse(400, "Validation failed", fieldErrors, LocalDateTime.now()));
	}

	/** 500 — unexpected error, hide internal details from client */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new ErrorResponse(status.value(), message, null, LocalDateTime.now()));
	}

	/**
	 * Standard error response structure returned for every error. Java record =
	 * immutable data class (concise syntax, Java 16+).
	 */
	public record ErrorResponse(int status, String message, Map<String, String> errors, LocalDateTime timestamp) {
	}
}