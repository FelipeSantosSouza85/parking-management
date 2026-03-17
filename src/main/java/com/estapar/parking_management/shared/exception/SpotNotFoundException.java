package com.estapar.parking_management.shared.exception;

/**
 * Exceção lançada quando uma vaga não é encontrada nas coordenadas informadas.
 */
public final class SpotNotFoundException extends ResourceNotFoundException {

    public SpotNotFoundException(Double lat, Double lng) {
        super("Spot not found at coordinates: lat=%s, lng=%s".formatted(lat, lng));
    }
}
