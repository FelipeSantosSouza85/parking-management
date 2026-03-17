package com.estapar.parking_management.garage.application.port;

import com.estapar.parking_management.garage.domain.GarageSector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port for GarageSector persistence operations.
 * Contract used by the application layer, implemented by infrastructure.
 */
public interface GarageSectorPort {

    GarageSector save(GarageSector sector);

    Optional<GarageSector> findById(Long id);

    Optional<GarageSector> findBySectorCode(String sectorCode);

    List<GarageSector> findAllBySectorCodeIn(Collection<String> sectorCodes);

    List<GarageSector> findAll();

    List<GarageSector> saveAll(List<GarageSector> sectors);
}
