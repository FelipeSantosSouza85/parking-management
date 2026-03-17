package com.estapar.parking_management.shared.exception;

/**
 * Exception thrown when a session state transition is invalid.
 */
public final class InvalidSessionTransitionException extends ConflictException {

    public InvalidSessionTransitionException(String from, String to) {
        super("Invalid session transition from %s to %s".formatted(from, to));
    }
}
