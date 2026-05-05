package com.app.taskmanagement.comment.exception;

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
 * Centralises all exception-to-HTTP-response mapping.
 *
 * Why here and not on the exceptions themselves (@ResponseStatus)?
 * 
 * @ResponseStatus on exceptions works but returns a plain Spring Whitelabel
 *                 error page which has inconsistent JSON structure. Handling
 *                 here guarantees every error returns a consistent { message,
 *                 timestamp } JSON body, which the frontend can rely on for all
 *                 error cases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/**
	 * Handles @Valid annotation failures (e.g. blank content, size exceeded).
	 * Returns all field-level validation errors at once so the client doesn't have
	 * to submit the form multiple times to discover all problems.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}
		return build(HttpStatus.BAD_REQUEST, "Validation failed: " + errors);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ErrorResponse(message, LocalDateTime.now()));
	}

	public record ErrorResponse(String message, LocalDateTime timestamp) {
	}
}