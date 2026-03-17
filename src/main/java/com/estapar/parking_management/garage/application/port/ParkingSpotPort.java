package com.estapar.parking_management.garage.application.port;

import com.estapar.parking_management.garage.domain.ParkingSpot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port for ParkingSpot persistence operations.
 * Contract used by the application layer, implemented by infrastructure.
 */
public interface ParkingSpotPort {

    ParkingSpot save(ParkingSpot spot);

    Optional<ParkingSpot> findById(Long id);

    Optional<ParkingSpot> findByLatAndLngWithLock(Double lat, Double lng);

    Optional<ParkingSpot> findByExternalSpotId(Integer externalSpotId);

    List<ParkingSpot> findAllByExternalSpotIdIn(Collection<Integer> externalSpotIds);

    List<ParkingSpot> saveAll(List<ParkingSpot> spots);
}
