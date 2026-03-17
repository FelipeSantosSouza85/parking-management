package com.estapar.parking_management.shared.exception;

/**
 * Exceção para payload inválido ou dados inconsistentes.
 * Mapeada para HTTP 400 Bad Request.
 */
public final class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
