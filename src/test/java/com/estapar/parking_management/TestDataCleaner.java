package com.estapar.parking_management;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageSectorJpaRepository;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.ParkingSpotJpaRepository;
import com.estapar.parking_management.parking.infrastructure.persistence.repository.ParkingSessionJpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper for data cleanup in integration tests.
 * Uses REQUIRES_NEW so cleanup is committed even when the test uses @Transactional with rollback.
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
