package com.estapar.parking_management.garage.infrastructure.persistence.repository;

import com.estapar.parking_management.garage.infrastructure.persistence.entity.ParkingSpotEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParkingSpotJpaRepository extends JpaRepository<ParkingSpotEntity, Long> {

    List<ParkingSpotEntity> findByExternalSpotIdIn(Collection<Integer> externalSpotIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSpotEntity s WHERE s.lat = :lat AND s.lng = :lng")
    Optional<ParkingSpotEntity> findByLatAndLngWithLock(@Param("lat") Double lat, @Param("lng") Double lng);

    Optional<ParkingSpotEntity> findByExternalSpotId(Integer externalSpotId);
}
