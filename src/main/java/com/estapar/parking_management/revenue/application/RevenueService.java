package com.estapar.parking_management.revenue.application;

import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.revenue.api.dto.RevenueResponse;
import com.estapar.parking_management.revenue.infrastructure.persistence.RevenueRepository;
import com.estapar.parking_management.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Serviço de aplicação que orquestra a consulta de receita por setor e data.
 */
@Service
public class RevenueService {

    private static final Logger log = LoggerFactory.getLogger(RevenueService.class);
    private static final String CURRENCY = "BRL";

    private final GarageSectorPort garageSectorPort;
    private final RevenueRepository revenueRepository;

    public RevenueService(GarageSectorPort garageSectorPort, RevenueRepository revenueRepository) {
        this.garageSectorPort = garageSectorPort;
        this.revenueRepository = revenueRepository;
    }

    @Transactional(readOnly = true)
    public RevenueResponse getRevenue(LocalDate date, String sectorCode) {
        GarageSector sector = garageSectorPort.findBySectorCode(sectorCode)
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + sectorCode));

        BigDecimal amount = revenueRepository.sumChargedAmount(sector.getId(), date);

        log.info("[REVENUE] - [RESULT] date={}, sector={}, amount={} {}", date, sectorCode, amount, CURRENCY);

        return new RevenueResponse(
                amount,
                CURRENCY,
                Instant.now()
        );
    }
}
