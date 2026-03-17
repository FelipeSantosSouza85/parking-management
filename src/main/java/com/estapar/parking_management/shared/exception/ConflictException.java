package com.estapar.parking_management.shared.exception;

/**
 * Exceção para conflito de estado ou regra de negócio.
 * Mapeada para HTTP 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
