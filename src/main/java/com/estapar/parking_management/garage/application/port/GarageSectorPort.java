package com.estapar.parking_management.garage.application.port;

import com.estapar.parking_management.garage.domain.GarageSector;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Port para operações de persistência de GarageSector.
 * Contrato usado pela camada de aplicação, implementado pela infraestrutura.
 */
public interface GarageSectorPort {

    GarageSector save(GarageSector sector);

    Optional<GarageSector> findById(Long id);

    Optional<GarageSector> findBySectorCode(String sectorCode);

    List<GarageSector> findAllBySectorCodeIn(Collection<String> sectorCodes);

    List<GarageSector> findAll();

    List<GarageSector> saveAll(List<GarageSector> sectors);
}
