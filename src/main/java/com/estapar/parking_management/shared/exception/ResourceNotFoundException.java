package com.estapar.parking_management.shared.exception;

/**
 * Exceção para recurso não encontrado.
 * Mapeada para HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
