package com.estapar.parking_management.shared.exception;

/**
 * Exception thrown when the garage is full and no spots are available.
 */
public final class GarageFullException extends ConflictException {

    public GarageFullException() {
        super("Garage is full");
    }

    public GarageFullException(String message) {
        super(message);
    }
}
