package com.estapar.parking_management.shared.exception;

/**
 * Exception thrown when a spot is already occupied.
 */
public final class SpotAlreadyOccupiedException extends ConflictException {

    public SpotAlreadyOccupiedException() {
        super("Spot is already occupied");
    }

    public SpotAlreadyOccupiedException(String message) {
        super(message);
    }
}
