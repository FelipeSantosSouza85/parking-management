package com.estapar.parking_management.garage.infrastructure.persistence.repository.adapter;

import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.infrastructure.persistence.mapper.GarageSectorMapper;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageSectorJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
public class GarageSectorAdapter implements GarageSectorPort {

    private final GarageSectorJpaRepository jpaRepository;
    private final GarageSectorMapper mapper;

    public GarageSectorAdapter(GarageSectorJpaRepository jpaRepository,
                              GarageSectorMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GarageSector save(GarageSector sector) {
        var entity = mapper.toEntity(sector);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<GarageSector> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<GarageSector> findBySectorCode(String sectorCode) {
        return jpaRepository.findBySectorCode(sectorCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<GarageSector> findAllBySectorCodeIn(Collection<String> sectorCodes) {
        if (sectorCodes == null || sectorCodes.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findBySectorCodeIn(sectorCodes).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<GarageSector> findAll() {
        return StreamSupport.stream(jpaRepository.findAll().spliterator(), false)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<GarageSector> saveAll(List<GarageSector> sectors) {
        if (sectors == null || sectors.isEmpty()) {
            return List.of();
        }
        var entities = sectors.stream()
                .map(mapper::toEntity)
                .toList();
        var saved = jpaRepository.saveAll(entities);
        return saved.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
