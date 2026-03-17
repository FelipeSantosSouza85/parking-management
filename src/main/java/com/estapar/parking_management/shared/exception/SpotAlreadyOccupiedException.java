package com.estapar.parking_management.shared.exception;

/**
 * Exceção lançada quando uma vaga já está ocupada.
 */
public final class SpotAlreadyOccupiedException extends ConflictException {

    public SpotAlreadyOccupiedException() {
        super("Spot is already occupied");
    }

    public SpotAlreadyOccupiedException(String message) {
        super(message);
    }
}
