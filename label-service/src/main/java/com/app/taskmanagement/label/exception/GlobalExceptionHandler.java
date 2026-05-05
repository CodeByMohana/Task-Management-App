package com.app.taskmanagement.label.exception;

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
 * Catches exceptions thrown anywhere in the application and converts them
 * into clean JSON error responses.
 *
 * Without this class, Spring would return its default "Whitelabel Error Page"
 * HTML, which is unusable for an API client (Postman / React frontend).
 *
 * HOW IT WORKS:
 *   When a controller or service throws an exception, Spring looks here
 *   for a matching @ExceptionHandler method. It calls that method and
 *   returns whatever ResponseEntity it produces.
 *
 * Example — throwing ResourceNotFoundException("Label not found: 99")
 * produces this HTTP response:
 *   HTTP 404
 *   { "message": "Label not found: 99", "timestamp": "2026-04-23T10:30:00" }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles label/checklist/item not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Handles invalid requests (e.g. duplicate label name, invalid operation)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles @Valid annotation failures on request DTOs.
     *
     * Example: sending { "name": "" } for creating a label fails @NotBlank.
     * This returns ALL validation errors at once so the client fixes everything in one go.
     *
     * Response example:
     * { "message": "Validation failed: {name=Label name must not be blank}", "timestamp": "..." }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Validation failed: " + errors);
    }

    // Catch-all for any unexpected error we didn't handle specifically
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Don't expose internal error details to the client — log them instead
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // Helper method to build a consistent error response body
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(message, LocalDateTime.now()));
    }

    /**
     * The JSON body of every error response.
     * Java record = immutable class with automatic getters and constructor.
     * Jackson (JSON library) automatically serializes this to JSON.
     */
    public record ErrorResponse(String message, LocalDateTime timestamp) {}
}