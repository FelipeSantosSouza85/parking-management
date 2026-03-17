package com.estapar.parking_management.garage.infrastructure.persistence.repository;

import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GarageSectorJpaRepository extends JpaRepository<GarageSectorEntity, Long> {

    Optional<GarageSectorEntity> findBySectorCode(String sectorCode);

    List<GarageSectorEntity> findBySectorCodeIn(Collection<String> sectorCodes);
}
