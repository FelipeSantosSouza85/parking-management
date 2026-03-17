package com.estapar.parking_management.shared.exception;

/**
 * Exceção lançada quando uma transição de estado de sessão é inválida.
 */
public final class InvalidSessionTransitionException extends ConflictException {

    public InvalidSessionTransitionException(String from, String to) {
        super("Invalid session transition from %s to %s".formatted(from, to));
    }
}
