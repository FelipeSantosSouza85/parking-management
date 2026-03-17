package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import com.estapar.parking_management.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Service responsible for processing vehicle exits from the garage.
 * Centralizes business rules for the EXIT use case.
 * Handles two scenarios: PARKED → EXITED (charges for time) and ENTERED → EXITED (zero charge).
 */
@Service
public class ExitVehicleService {

    private static final Logger log = LoggerFactory.getLogger(ExitVehicleService.class);

    private final ParkingSessionPort parkingSessionPort;
    private final ParkingSpotPort parkingSpotPort;
    private final GarageOccupancyPort garageOccupancyPort;
    private final PricingCalculator pricingCalculator;

    public ExitVehicleService(
            ParkingSessionPort parkingSessionPort,
            ParkingSpotPort parkingSpotPort,
            GarageOccupancyPort garageOccupancyPort,
            PricingCalculator pricingCalculator
    ) {
        this.parkingSessionPort = parkingSessionPort;
        this.parkingSpotPort = parkingSpotPort;
        this.garageOccupancyPort = garageOccupancyPort;
        this.pricingCalculator = pricingCalculator;
    }

    /**
     * Processes a vehicle exit from the garage.
     * Full flow within a single transaction.
     *
     * @param licensePlate vehicle license plate
     * @param exitTime     exit date/time (converted to Instant UTC internally)
     * @throws ValidationException                if licensePlate or exitTime are invalid
     * @throws ActiveSessionNotFoundException    if no active session exists for the plate
     * @throws IllegalStateException             if GarageOccupancy is not found
     */
    @Transactional
    public void processExit(String licensePlate, LocalDateTime exitTime) {
        validateExitInput(licensePlate, exitTime);
        Instant exitInstant = exitTime.toInstant(ZoneOffset.UTC);

        log.info("[PARKING] - [EXIT] plate={}, exitTime={}", licensePlate, exitInstant);

        ParkingSession session = parkingSessionPort.findActiveByLicensePlateWithLock(licensePlate)
                .orElseThrow(() -> new ActiveSessionNotFoundException(licensePlate));

        session.setExitTime(exitInstant);

        if (session.getStatus() == ParkingSessionStatus.PARKED) {
            Duration duration = Duration.between(session.getEntryTime(), exitInstant);
            int chargeableHours = pricingCalculator.calculateChargeableHours(duration);
            BigDecimal chargedAmount = pricingCalculator.calculateChargedAmount(
                    session.getHourlyPriceApplied(), chargeableHours);
            session.setChargedAmount(chargedAmount);

            ParkingSpot spot = session.getParkingSpot();
            spot.release();
            parkingSpotPort.save(spot);
        } else {
            // ENTERED -> EXITED: no parking, zero charge
            session.setChargedAmount(BigDecimal.ZERO);
        }

        session.setStatus(ParkingSessionStatus.EXITED);

        GarageOccupancy occupancy = garageOccupancyPort.findWithLock()
                .orElseThrow(() -> new IllegalStateException("GarageOccupancy not found"));
        occupancy.decrementOccupied();
        garageOccupancyPort.save(occupancy);

        parkingSessionPort.save(session);

        log.info("[PARKING] - [EXIT_COMPLETED] plate={}, status={}, chargedAmount={}",
                licensePlate, session.getStatus(), session.getChargedAmount());
    }

    private void validateExitInput(String licensePlate, LocalDateTime exitTime) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new ValidationException("licensePlate must not be null or blank");
        }
        if (exitTime == null) {
            throw new ValidationException("exitTime must not be null");
        }
    }
}
