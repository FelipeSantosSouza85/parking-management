package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionAlreadyExistsException;
import com.estapar.parking_management.shared.exception.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Service responsible for processing vehicle entries into the garage.
 * Centralizes business rules for the ENTRY use case.
 */
@Service
public class EntryVehicleService {

    private static final Logger log = LoggerFactory.getLogger(EntryVehicleService.class);

    private final ParkingSessionPort parkingSessionPort;
    private final GarageOccupancyPort garageOccupancyPort;
    private final PricingAdjustmentPolicy pricingAdjustmentPolicy;

    public EntryVehicleService(
            ParkingSessionPort parkingSessionPort,
            GarageOccupancyPort garageOccupancyPort,
            PricingAdjustmentPolicy pricingAdjustmentPolicy
    ) {
        this.parkingSessionPort = parkingSessionPort;
        this.garageOccupancyPort = garageOccupancyPort;
        this.pricingAdjustmentPolicy = pricingAdjustmentPolicy;
    }

    /**
     * Processes a vehicle entry into the garage.
     * Full flow within a single transaction.
     *
     * @param licensePlate vehicle license plate
     * @param entryTime    entry date/time (converted to Instant UTC internally)
     * @throws ValidationException                  if licensePlate or entryTime are invalid
     * @throws ActiveSessionAlreadyExistsException  if an active session already exists for the plate
     * @throws IllegalStateException                if GarageOccupancy is not found
     * @throws com.estapar.parking_management.shared.exception.GarageFullException if the garage is full
     */
    @Transactional
    public void processEntry(String licensePlate, LocalDateTime entryTime) {
        validateEntryInput(licensePlate, entryTime);
        Instant entryInstant = entryTime.toInstant(ZoneOffset.UTC);

        log.info("[PARKING] - [ENTRY] plate={}, time={}", licensePlate, entryInstant);
        
        if (parkingSessionPort.existsActiveByLicensePlate(licensePlate)) {
            throw new ActiveSessionAlreadyExistsException(licensePlate);
        }

        GarageOccupancy occupancy = garageOccupancyPort.findWithLock()
                .orElseThrow(() -> new IllegalStateException("GarageOccupancy not found"));

        BigDecimal occupancyRate = occupancy.getOccupancyRate();
        BigDecimal priceAdjustmentRate = pricingAdjustmentPolicy.getAdjustmentRate(occupancyRate);

        occupancy.incrementOccupied();
        garageOccupancyPort.save(occupancy);

        ParkingSession session = new ParkingSession(licensePlate, entryInstant, ParkingSessionStatus.ENTERED);
        session.setOccupancyRateAtEntry(occupancyRate);
        session.setPriceAdjustmentRateAtEntry(priceAdjustmentRate);

        parkingSessionPort.save(session);
    }

    private void validateEntryInput(String licensePlate, LocalDateTime entryTime) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new ValidationException("licensePlate must not be null or blank");
        }
        if (entryTime == null) {
            throw new ValidationException("entryTime must not be null");
        }
    }
}
