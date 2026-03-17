package com.estapar.parking_management.shared.exception;

/**
 * Exception thrown when no active session exists for the given license plate.
 */
public final class ActiveSessionNotFoundException extends ResourceNotFoundException {

    public ActiveSessionNotFoundException(String licensePlate) {
        super("Active session not found for license plate: %s".formatted(licensePlate));
    }
}
