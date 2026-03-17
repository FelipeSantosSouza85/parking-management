package com.estapar.parking_management.shared.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Centralized exception handling for the REST API.
 * Converts exceptions into standardized HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(ValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is not present";
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(GarageFullException.class)
    public ResponseEntity<ApiErrorResponse> handleGarageFullException(GarageFullException ex) {
        return buildResponse(HttpStatus.CONFLICT, "GARAGE_FULL", ex.getMessage());
    }

    @ExceptionHandler(SpotAlreadyOccupiedException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotAlreadyOccupiedException(SpotAlreadyOccupiedException ex) {
        return buildResponse(HttpStatus.CONFLICT, "SPOT_ALREADY_OCCUPIED", ex.getMessage());
    }

    @ExceptionHandler(ActiveSessionAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleActiveSessionAlreadyExistsException(ActiveSessionAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, "ACTIVE_SESSION_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(InvalidSessionTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSessionTransitionException(InvalidSessionTransitionException ex) {
        return buildResponse(HttpStatus.CONFLICT, "INVALID_SESSION_TRANSITION", ex.getMessage());
    }

    @ExceptionHandler(SpotNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotNotFoundException(SpotNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "SPOT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ActiveSessionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleActiveSessionNotFoundException(ActiveSessionNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "ACTIVE_SESSION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflictException(ConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private static ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(new ApiErrorResponse(code, message, Instant.now()));
    }
}
