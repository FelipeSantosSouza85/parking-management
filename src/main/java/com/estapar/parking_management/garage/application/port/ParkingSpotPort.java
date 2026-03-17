package com.estapar.parking_management.garage.application.port;

import com.estapar.parking_management.garage.domain.ParkingSpot;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port para operações de persistência de ParkingSpot.
 * Contrato usado pela camada de aplicação, implementado pela infraestrutura.
 */
public interface ParkingSpotPort {

    ParkingSpot save(ParkingSpot spot);

    Optional<ParkingSpot> findById(Long id);

    Optional<ParkingSpot> findByLatAndLngWithLock(Double lat, Double lng);

    Optional<ParkingSpot> findByExternalSpotId(Integer externalSpotId);

    List<ParkingSpot> findAllByExternalSpotIdIn(Collection<Integer> externalSpotIds);

    List<ParkingSpot> saveAll(List<ParkingSpot> spots);
}
