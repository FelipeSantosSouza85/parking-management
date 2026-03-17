package com.estapar.parking_management.shared.exception;

/**
 * Exceção lançada quando não existe sessão ativa para a placa informada.
 */
public final class ActiveSessionNotFoundException extends ResourceNotFoundException {

    public ActiveSessionNotFoundException(String licensePlate) {
        super("Active session not found for license plate: %s".formatted(licensePlate));
    }
}
