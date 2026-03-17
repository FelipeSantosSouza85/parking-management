package com.estapar.parking_management.garage.application.port;

import com.estapar.parking_management.garage.domain.GarageOccupancy;

import java.util.Optional;

/**
 * Port para operações de persistência de GarageOccupancy.
 * Contrato usado pela camada de aplicação, implementado pela infraestrutura.
 */
public interface GarageOccupancyPort {

    Optional<GarageOccupancy> findWithLock();

    Optional<GarageOccupancy> findById(Long id);

    Optional<GarageOccupancy> findByIdWithLock(Long id);

    GarageOccupancy save(GarageOccupancy occupancy);

    void deleteAll();
}
