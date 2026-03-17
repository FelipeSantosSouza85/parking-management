package com.estapar.parking_management.shared.exception;

/**
 * Exception thrown when a spot is not found at the given coordinates.
 */
public final class SpotNotFoundException extends ResourceNotFoundException {

    public SpotNotFoundException(Double lat, Double lng) {
        super("Spot not found at coordinates: lat=%s, lng=%s".formatted(lat, lng));
    }
}
