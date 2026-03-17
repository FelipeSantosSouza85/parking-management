package com.estapar.parking_management.parking.application.port;

import com.estapar.parking_management.parking.domain.ParkingSession;

import java.util.Optional;

/**
 * Port for ParkingSession persistence operations.
 * Contract used by the application layer, implemented by infrastructure.
 */
public interface ParkingSessionPort {

    ParkingSession save(ParkingSession session);

    Optional<ParkingSession> findById(Long id);

    Optional<ParkingSession> findActiveByLicensePlateWithLock(String licensePlate);

    boolean existsActiveByLicensePlate(String licensePlate);
}
