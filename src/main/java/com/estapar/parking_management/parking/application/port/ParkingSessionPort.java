package com.estapar.parking_management.parking.application.port;

import com.estapar.parking_management.parking.domain.ParkingSession;

import java.util.Optional;

/**
 * Port para operações de persistência de ParkingSession.
 * Contrato usado pela camada de aplicação, implementado pela infraestrutura.
 */
public interface ParkingSessionPort {

    ParkingSession save(ParkingSession session);

    Optional<ParkingSession> findById(Long id);

    Optional<ParkingSession> findActiveByLicensePlateWithLock(String licensePlate);

    boolean existsActiveByLicensePlate(String licensePlate);
}
