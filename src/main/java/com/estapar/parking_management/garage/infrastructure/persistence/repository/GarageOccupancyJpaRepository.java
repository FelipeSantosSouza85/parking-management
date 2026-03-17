package com.estapar.parking_management.garage.infrastructure.persistence.repository;

import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageOccupancyEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GarageOccupancyJpaRepository extends JpaRepository<GarageOccupancyEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GarageOccupancyEntity> findFirstByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM GarageOccupancyEntity o WHERE o.id = :id")
    Optional<GarageOccupancyEntity> findByIdWithLock(@Param("id") Long id);
}
