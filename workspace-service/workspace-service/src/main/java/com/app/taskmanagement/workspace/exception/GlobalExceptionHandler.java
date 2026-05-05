package com.app.taskmanagement.workspace.exception;

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
 * Centralized error handling for the entire workspace-service.
 *
 * @RestControllerAdvice intercepts exceptions thrown by any controller and
 *                       converts them into clean JSON error responses instead
 *                       of stack traces.
 *
 *                       Without this, Spring would return a raw 500 error page
 *                       with HTML — not helpful for APIs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 404 — resource (workspace, member) was not found in the DB.
	 */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/**
	 * 409 — trying to add a member who is already in the workspace, etc.
	 */
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * 400 — invalid input that isn't caught by bean validation.
	 */
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	/**
	 * 403 — user is authenticated but doesn't have permission for this action.
	 * Example: a MEMBER trying to delete the workspace (only ADMIN can do that).
	 */
	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
		return build(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	/**
	 * 400 — handles @Valid annotation failures on request body fields. Returns a
	 * map of field → error message so the frontend knows which field failed.
	 * Example: { "name": "Workspace name is required" }
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		ErrorResponse body = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors,
				LocalDateTime.now());
		return ResponseEntity.badRequest().body(body);
	}

	/**
	 * 500 — catch-all for any unexpected exceptions. We hide the internal message
	 * to avoid leaking implementation details.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	// ─── Helper to build a consistent response body ───────────────────────────

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new ErrorResponse(status.value(), message, null, LocalDateTime.now()));
	}

	/**
	 * The JSON structure returned for every error. Using Java 'record' — an
	 * immutable data class (Java 16+).
	 */
	public record ErrorResponse(int status, String message, Map<String, String> errors, LocalDateTime timestamp) {
	}
}