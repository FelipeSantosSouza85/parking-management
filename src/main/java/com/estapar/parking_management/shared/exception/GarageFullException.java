package com.estapar.parking_management.shared.exception;

/**
 * Exceção lançada quando a garagem está cheia e não há vagas disponíveis.
 */
public final class GarageFullException extends ConflictException {

    public GarageFullException() {
        super("Garage is full");
    }

    public GarageFullException(String message) {
        super(message);
    }
}
