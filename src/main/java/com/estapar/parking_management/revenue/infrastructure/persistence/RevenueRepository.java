package com.estapar.parking_management.revenue.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Repository;

import com.estapar.parking_management.parking.domain.ParkingSessionStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Repositório de query para agregação de receita por setor e data.
 * Consulta diretamente a tabela parking_session filtrando por status EXITED.
 */
@Repository
public class RevenueRepository {

    private static final String JPQL = """
            SELECT COALESCE(SUM(s.chargedAmount), 0)
            FROM ParkingSessionEntity s
            WHERE s.sector.id = :sectorId
              AND s.status = :status
              AND s.exitTime >= :startOfDay
              AND s.exitTime < :startOfNextDay
            """;

    private final EntityManager entityManager;

    public RevenueRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public BigDecimal sumChargedAmount(Long sectorId, LocalDate date) {
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        TypedQuery<BigDecimal> query = entityManager.createQuery(JPQL, BigDecimal.class);
        query.setParameter("sectorId", sectorId);
        query.setParameter("status", ParkingSessionStatus.EXITED);
        query.setParameter("startOfDay", startOfDay);
        query.setParameter("startOfNextDay", startOfNextDay);

        BigDecimal result = query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}
