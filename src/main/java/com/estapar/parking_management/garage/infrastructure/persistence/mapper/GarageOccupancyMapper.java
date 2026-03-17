package com.estapar.parking_management.garage.infrastructure.persistence.mapper;

import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageOccupancyEntity;
import org.springframework.stereotype.Component;

@Component
public class GarageOccupancyMapper {

    public GarageOccupancy toDomain(GarageOccupancyEntity entity) {
        if (entity == null) {
            return null;
        }
        GarageOccupancy domain = new GarageOccupancy(entity.getTotalCapacity());
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        domain.setOccupiedCount(entity.getOccupiedCount() != null ? entity.getOccupiedCount() : 0);
        return domain;
    }

    public GarageOccupancyEntity toEntity(GarageOccupancy domain) {
        if (domain == null) {
            return null;
        }
        GarageOccupancyEntity entity = new GarageOccupancyEntity();
        entity.setId(domain.getId());
        entity.setTotalCapacity(domain.getTotalCapacity());
        entity.setOccupiedCount(domain.getOccupiedCount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    /**
     * Updates the entity state with domain data (for merge/update).
     * Does not change id, createdAt.
     */
    public void updateEntity(GarageOccupancyEntity entity, GarageOccupancy domain) {
        if (entity == null || domain == null) {
            return;
        }
        entity.setTotalCapacity(domain.getTotalCapacity());
        entity.setOccupiedCount(domain.getOccupiedCount());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }
}
