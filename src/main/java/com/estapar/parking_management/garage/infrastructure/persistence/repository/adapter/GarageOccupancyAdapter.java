package com.estapar.parking_management.garage.infrastructure.persistence.repository.adapter;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageOccupancyEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.mapper.GarageOccupancyMapper;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageOccupancyJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GarageOccupancyAdapter implements GarageOccupancyPort {

    private final GarageOccupancyJpaRepository jpaRepository;
    private final GarageOccupancyMapper mapper;

    public GarageOccupancyAdapter(GarageOccupancyJpaRepository jpaRepository,
                                 GarageOccupancyMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<GarageOccupancy> findWithLock() {
        return jpaRepository.findFirstByOrderByIdAsc()
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GarageOccupancy> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GarageOccupancy> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id)
                .map(mapper::toDomain);
    }

    @Override
    public GarageOccupancy save(GarageOccupancy occupancy) {
        GarageOccupancyEntity entity = mapper.toEntity(occupancy);
        GarageOccupancyEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }
}
