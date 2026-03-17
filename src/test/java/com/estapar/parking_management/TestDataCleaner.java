package com.estapar.parking_management;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageSectorJpaRepository;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.ParkingSpotJpaRepository;
import com.estapar.parking_management.parking.infrastructure.persistence.repository.ParkingSessionJpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper para limpeza de dados em testes de integração.
 * Usa REQUIRES_NEW para que a limpeza seja commitada mesmo quando o teste usa @Transactional com rollback.
 */
public class TestDataCleaner {

    private final ParkingSessionJpaRepository parkingSessionJpaRepository;
    private final ParkingSpotJpaRepository parkingSpotJpaRepository;
    private final GarageSectorJpaRepository garageSectorJpaRepository;
    private final GarageOccupancyPort garageOccupancyPort;

    public TestDataCleaner(
            ParkingSessionJpaRepository parkingSessionJpaRepository,
            ParkingSpotJpaRepository parkingSpotJpaRepository,
            GarageSectorJpaRepository garageSectorJpaRepository,
            GarageOccupancyPort garageOccupancyPort
    ) {
        this.parkingSessionJpaRepository = parkingSessionJpaRepository;
        this.parkingSpotJpaRepository = parkingSpotJpaRepository;
        this.garageSectorJpaRepository = garageSectorJpaRepository;
        this.garageOccupancyPort = garageOccupancyPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanAll() {
        parkingSessionJpaRepository.deleteAll();
        parkingSpotJpaRepository.deleteAll();
        garageSectorJpaRepository.deleteAll();
        garageOccupancyPort.deleteAll();
    }
}
