package com.estapar.parking_management.parking.infrastructure.persistence.repository;

import com.estapar.parking_management.parking.infrastructure.persistence.entity.ParkingSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParkingSessionJpaRepository extends JpaRepository<ParkingSessionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSessionEntity s WHERE s.licensePlate = :plate AND s.status IN ('ENTERED', 'PARKED')")
    Optional<ParkingSessionEntity> findActiveByLicensePlateWithLock(@Param("plate") String licensePlate);

    @Query("SELECT COUNT(s) > 0 FROM ParkingSessionEntity s WHERE s.licensePlate = :plate AND s.status IN ('ENTERED', 'PARKED')")
    boolean existsActiveByLicensePlate(@Param("plate") String licensePlate);
}
