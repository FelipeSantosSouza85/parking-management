package com.estapar.parking_management.shared.exception;

/**
 * Exception thrown when an active session already exists for the given license plate.
 */
public final class ActiveSessionAlreadyExistsException extends ConflictException {

    public ActiveSessionAlreadyExistsException(String licensePlate) {
        super("Active session already exists for license plate: %s".formatted(licensePlate));
    }
}
