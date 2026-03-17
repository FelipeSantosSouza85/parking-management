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
 * Serviço responsável por processar a saída de veículos da garagem.
 * Centraliza as regras de negócio do use case EXIT.
 * Lida com dois cenários: PARKED → EXITED (cobra pelo tempo) e ENTERED → EXITED (cobrança zero).
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
     * Processa a saída de um veículo da garagem.
     * Fluxo completo dentro de uma única transação.
     *
     * @param licensePlate placa do veículo
     * @param exitTime     data/hora da saída (convertida para Instant UTC internamente)
     * @throws ValidationException                se licensePlate ou exitTime forem inválidos
     * @throws ActiveSessionNotFoundException    se não existir sessão ativa para a placa
     * @throws IllegalStateException             se GarageOccupancy não for encontrada
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
            // ENTERED -> EXITED: sem estacionar, cobrança zero
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
