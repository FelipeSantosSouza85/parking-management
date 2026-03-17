package com.estapar.parking_management.parking.infrastructure.persistence.mapper;

import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.ParkingSpotEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.mapper.GarageSectorMapper;
import com.estapar.parking_management.garage.infrastructure.persistence.mapper.ParkingSpotMapper;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionMapper {

    private final ParkingSpotMapper parkingSpotMapper;
    private final GarageSectorMapper garageSectorMapper;

    public ParkingSessionMapper(ParkingSpotMapper parkingSpotMapper, GarageSectorMapper garageSectorMapper) {
        this.parkingSpotMapper = parkingSpotMapper;
        this.garageSectorMapper = garageSectorMapper;
    }

    public ParkingSession toDomain(ParkingSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        ParkingSession domain = new ParkingSession(
                entity.getLicensePlate(),
                entity.getEntryTime(),
                entity.getStatus()
        );
        domain.setId(entity.getId());
        domain.setParkedTime(entity.getParkedTime());
        domain.setExitTime(entity.getExitTime());
        domain.setOccupancyRateAtEntry(entity.getOccupancyRateAtEntry());
        domain.setPriceAdjustmentRateAtEntry(entity.getPriceAdjustmentRateAtEntry());
        domain.setHourlyPriceApplied(entity.getHourlyPriceApplied());
        domain.setChargedAmount(entity.getChargedAmount());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());

        ParkingSpot spot = parkingSpotMapper.toDomain(entity.getParkingSpot());
        domain.setParkingSpot(spot);

        GarageSector sector = garageSectorMapper.toDomain(entity.getSector());
        domain.setSector(sector);

        return domain;
    }

    /**
     * Converte domain para entity. O parkingSpotEntity e sectorEntity devem ser
     * referências JPA já persistidas (obtidas via repositório) para evitar duplicação.
     * Ambos podem ser null quando a sessão não está em status PARKED.
     */
    public ParkingSessionEntity toEntity(ParkingSession domain,
                                          ParkingSpotEntity parkingSpotEntity,
                                          GarageSectorEntity sectorEntity) {
        if (domain == null) {
            return null;
        }
        ParkingSessionEntity entity = new ParkingSessionEntity();
        entity.setId(domain.getId());
        entity.setLicensePlate(domain.getLicensePlate());
        entity.setStatus(domain.getStatus());
        entity.setEntryTime(domain.getEntryTime());
        entity.setParkedTime(domain.getParkedTime());
        entity.setExitTime(domain.getExitTime());
        entity.setParkingSpot(parkingSpotEntity);
        entity.setSector(sectorEntity);
        entity.setOccupancyRateAtEntry(domain.getOccupancyRateAtEntry());
        entity.setPriceAdjustmentRateAtEntry(domain.getPriceAdjustmentRateAtEntry());
        entity.setHourlyPriceApplied(domain.getHourlyPriceApplied());
        entity.setChargedAmount(domain.getChargedAmount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
