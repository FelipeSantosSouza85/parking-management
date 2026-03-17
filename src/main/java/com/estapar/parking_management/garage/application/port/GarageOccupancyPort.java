package com.estapar.parking_management.garage.application.port;

import com.estapar.parking_management.garage.domain.GarageOccupancy;

import java.util.Optional;

/**
 * Port for GarageOccupancy persistence operations.
 * Contract used by the application layer, implemented by infrastructure.
 */
public interface GarageOccupancyPort {

    Optional<GarageOccupancy> findWithLock();

    Optional<GarageOccupancy> findById(Long id);

    Optional<GarageOccupancy> findByIdWithLock(Long id);

    GarageOccupancy save(GarageOccupancy occupancy);

    void deleteAll();
}
