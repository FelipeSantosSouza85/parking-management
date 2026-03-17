package com.estapar.parking_management.parking.infrastructure.persistence.repository.adapter;

import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.ParkingSpotEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageSectorJpaRepository;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.ParkingSpotJpaRepository;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import com.estapar.parking_management.parking.infrastructure.persistence.mapper.ParkingSessionMapper;
import com.estapar.parking_management.parking.infrastructure.persistence.repository.ParkingSessionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ParkingSessionAdapter implements ParkingSessionPort {

    private final ParkingSessionJpaRepository jpaRepository;
    private final ParkingSpotJpaRepository parkingSpotJpaRepository;
    private final GarageSectorJpaRepository garageSectorJpaRepository;
    private final ParkingSessionMapper mapper;

    public ParkingSessionAdapter(ParkingSessionJpaRepository jpaRepository,
                                ParkingSpotJpaRepository parkingSpotJpaRepository,
                                GarageSectorJpaRepository garageSectorJpaRepository,
                                ParkingSessionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.parkingSpotJpaRepository = parkingSpotJpaRepository;
        this.garageSectorJpaRepository = garageSectorJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ParkingSession save(ParkingSession session) {
        ParkingSpotEntity spotEntity = null;
        GarageSectorEntity sectorEntity = null;

        if (session.getParkingSpot() != null) {
            spotEntity = parkingSpotJpaRepository.findById(session.getParkingSpot().getId())
                    .orElseThrow(() -> new IllegalStateException("ParkingSpot not found: " + session.getParkingSpot().getId()));
        }
        if (session.getSector() != null) {
            sectorEntity = garageSectorJpaRepository.findById(session.getSector().getId())
                    .orElseThrow(() -> new IllegalStateException("GarageSector not found: " + session.getSector().getId()));
        }

        ParkingSessionEntity entity = mapper.toEntity(session, spotEntity, sectorEntity);
        ParkingSessionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ParkingSession> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ParkingSession> findActiveByLicensePlateWithLock(String licensePlate) {
        return jpaRepository.findActiveByLicensePlateWithLock(licensePlate)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByLicensePlate(String licensePlate) {
        return jpaRepository.existsActiveByLicensePlate(licensePlate);
    }
}
