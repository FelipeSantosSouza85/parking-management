package com.estapar.parking_management.shared.exception;

/**
 * Exceção lançada quando já existe uma sessão ativa para a placa informada.
 */
public final class ActiveSessionAlreadyExistsException extends ConflictException {

    public ActiveSessionAlreadyExistsException(String licensePlate) {
        super("Active session already exists for license plate: %s".formatted(licensePlate));
    }
}
