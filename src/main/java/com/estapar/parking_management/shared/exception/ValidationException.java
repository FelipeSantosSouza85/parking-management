package com.estapar.parking_management.shared.exception;

/**
 * Exception for invalid payload or inconsistent data.
 * Mapped to HTTP 400 Bad Request.
 */
public final class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
