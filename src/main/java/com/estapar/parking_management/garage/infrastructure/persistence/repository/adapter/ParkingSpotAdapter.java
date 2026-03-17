package com.estapar.parking_management.garage.infrastructure.persistence.repository.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.mapper.ParkingSpotMapper;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageSectorJpaRepository;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.ParkingSpotJpaRepository;

@Repository
public class ParkingSpotAdapter implements ParkingSpotPort {

    private final ParkingSpotJpaRepository jpaRepository;
    private final GarageSectorJpaRepository sectorJpaRepository;
    private final ParkingSpotMapper mapper;

    public ParkingSpotAdapter(ParkingSpotJpaRepository jpaRepository,
                             GarageSectorJpaRepository sectorJpaRepository,
                             ParkingSpotMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.sectorJpaRepository = sectorJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ParkingSpot save(ParkingSpot spot) {
        GarageSectorEntity sectorEntity = sectorJpaRepository.findById(spot.getSector().getId())
                .orElseThrow(() -> new IllegalStateException("Sector not found: " + spot.getSector().getId()));
        var entity = mapper.toEntity(spot, sectorEntity);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ParkingSpot> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ParkingSpot> findByLatAndLngWithLock(Double lat, Double lng) {
        return jpaRepository.findByLatAndLngWithLock(lat, lng)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ParkingSpot> findByExternalSpotId(Integer externalSpotId) {
        return jpaRepository.findByExternalSpotId(externalSpotId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ParkingSpot> findAllByExternalSpotIdIn(Collection<Integer> externalSpotIds) {
        if (externalSpotIds == null || externalSpotIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findByExternalSpotIdIn(externalSpotIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ParkingSpot> saveAll(List<ParkingSpot> spots) {
        if (spots == null || spots.isEmpty()) {
            return List.of();
        }
        var sectorIds = spots.stream()
                .map(spot -> spot.getSector().getId())
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, GarageSectorEntity> sectorById = sectorJpaRepository.findAllById(sectorIds).stream()
                .collect(Collectors.toMap(GarageSectorEntity::getId, e -> e));
        var entities = spots.stream()
                .map(spot -> {
                    var sectorEntity = sectorById.get(spot.getSector().getId());
                    if (sectorEntity == null) {
                        throw new IllegalStateException("Sector not found: " + spot.getSector().getId());
                    }
                    return mapper.toEntity(spot, sectorEntity);
                })
                .toList();
        var saved = jpaRepository.saveAll(entities);
        return saved.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
