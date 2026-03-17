package com.estapar.parking_management.garage.infrastructure.persistence.mapper;

import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.ParkingSpotEntity;
import org.springframework.stereotype.Component;

@Component
public class ParkingSpotMapper {

    private final GarageSectorMapper garageSectorMapper;

    public ParkingSpotMapper(GarageSectorMapper garageSectorMapper) {
        this.garageSectorMapper = garageSectorMapper;
    }

    public ParkingSpot toDomain(ParkingSpotEntity entity) {
        if (entity == null) {
            return null;
        }
        GarageSector sector = garageSectorMapper.toDomain(entity.getSector());
        ParkingSpot domain = new ParkingSpot(
                entity.getExternalSpotId(),
                sector,
                entity.getLat(),
                entity.getLng(),
                entity.isOccupied()
        );
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    /**
     * Converte domain para entity. O sectorEntity deve ser a referência JPA
     * já persistida (obtida via repositório) para evitar duplicação de setores.
     */
    public ParkingSpotEntity toEntity(ParkingSpot domain, GarageSectorEntity sectorEntity) {
        if (domain == null) {
            return null;
        }
        ParkingSpotEntity entity = new ParkingSpotEntity();
        entity.setId(domain.getId());
        entity.setExternalSpotId(domain.getExternalSpotId());
        entity.setSector(sectorEntity);
        entity.setLat(domain.getLat());
        entity.setLng(domain.getLng());
        entity.setOccupied(domain.isOccupied());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
