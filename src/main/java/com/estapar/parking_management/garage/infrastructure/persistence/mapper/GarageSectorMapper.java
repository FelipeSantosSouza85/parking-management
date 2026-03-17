package com.estapar.parking_management.garage.infrastructure.persistence.mapper;

import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import org.springframework.stereotype.Component;

@Component
public class GarageSectorMapper {

    public GarageSector toDomain(GarageSectorEntity entity) {
        if (entity == null) {
            return null;
        }
        GarageSector domain = new GarageSector(
                entity.getSectorCode(),
                entity.getBasePrice(),
                entity.getMaxCapacity(),
                entity.getOpenHour(),
                entity.getCloseHour(),
                entity.getDurationLimitMinutes()
        );
        domain.setId(entity.getId());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public GarageSectorEntity toEntity(GarageSector domain) {
        if (domain == null) {
            return null;
        }
        GarageSectorEntity entity = new GarageSectorEntity();
        entity.setId(domain.getId());
        entity.setSectorCode(domain.getSectorCode());
        entity.setBasePrice(domain.getBasePrice());
        entity.setMaxCapacity(domain.getMaxCapacity());
        entity.setOpenHour(domain.getOpenHour());
        entity.setCloseHour(domain.getCloseHour());
        entity.setDurationLimitMinutes(domain.getDurationLimitMinutes());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
