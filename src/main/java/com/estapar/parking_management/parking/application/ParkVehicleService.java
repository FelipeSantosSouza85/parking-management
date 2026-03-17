package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import com.estapar.parking_management.shared.exception.InvalidSessionTransitionException;
import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;
import com.estapar.parking_management.shared.exception.SpotNotFoundException;
import com.estapar.parking_management.shared.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Serviço responsável por processar o evento PARKED.
 * Associa o veículo (com sessão ENTERED) a uma vaga física identificada por coordenadas lat/lng.
 */
@Service
public class ParkVehicleService {

    private static final Logger log = LoggerFactory.getLogger(ParkVehicleService.class);

    private final ParkingSessionPort parkingSessionPort;
    private final ParkingSpotPort parkingSpotPort;
    private final PricingCalculator pricingCalculator;

    public ParkVehicleService(
            ParkingSessionPort parkingSessionPort,
            ParkingSpotPort parkingSpotPort,
            PricingCalculator pricingCalculator
    ) {
        this.parkingSessionPort = parkingSessionPort;
        this.parkingSpotPort = parkingSpotPort;
        this.pricingCalculator = pricingCalculator;
    }

    /**
     * Processa o evento PARKED: associa a sessão ativa à vaga nas coordenadas informadas.
     *
     * @param licensePlate placa do veículo
     * @param lat          latitude da vaga
     * @param lng          longitude da vaga
     * @throws ValidationException                   se licensePlate, lat ou lng forem inválidos
     * @throws ActiveSessionNotFoundException       se não existir sessão ativa para a placa
     * @throws InvalidSessionTransitionException     se o status da sessão não for ENTERED
     * @throws SpotNotFoundException                 se não existir vaga nas coordenadas
     * @throws SpotAlreadyOccupiedException          se a vaga já estiver ocupada
     */
    @Transactional
    public void processParked(String licensePlate, Double lat, Double lng) {
        validateParkedInput(licensePlate, lat, lng);

        log.info("[PARKING] - [PARKED] plate={}, lat={}, lng={}", licensePlate, lat, lng);

        ParkingSession session = parkingSessionPort.findActiveByLicensePlateWithLock(licensePlate)
                .orElseThrow(() -> new ActiveSessionNotFoundException(licensePlate));

        if (session.getStatus() != ParkingSessionStatus.ENTERED) {
            throw new InvalidSessionTransitionException(session.getStatus().name(), "PARKED");
        }

        ParkingSpot spot = parkingSpotPort.findByLatAndLngWithLock(lat, lng)
                .orElseThrow(() -> new SpotNotFoundException(lat, lng));

        spot.occupy();
        parkingSpotPort.save(spot);

        session.setParkingSpot(spot);
        session.setSector(spot.getSector());
        session.setParkedTime(Instant.now());

        BigDecimal hourlyPrice = pricingCalculator.calculateHourlyPrice(
                spot.getSector().getBasePrice(),
                session.getPriceAdjustmentRateAtEntry()
        );
        session.setHourlyPriceApplied(hourlyPrice);
        session.setStatus(ParkingSessionStatus.PARKED);

        parkingSessionPort.save(session);

        log.info("[PARKING] - [PARKED_COMPLETED] plate={}, spotId={}, sector={}",
                licensePlate, spot.getExternalSpotId(), spot.getSector().getSectorCode());
    }

    private void validateParkedInput(String licensePlate, Double lat, Double lng) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new ValidationException("licensePlate must not be null or blank");
        }
        if (lat == null) {
            throw new ValidationException("lat must not be null");
        }
        if (lng == null) {
            throw new ValidationException("lng must not be null");
        }
    }
}
