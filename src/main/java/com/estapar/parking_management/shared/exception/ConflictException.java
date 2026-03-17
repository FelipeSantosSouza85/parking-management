package com.estapar.parking_management.shared.exception;

/**
 * Exception for state or business rule conflict.
 * Mapped to HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
