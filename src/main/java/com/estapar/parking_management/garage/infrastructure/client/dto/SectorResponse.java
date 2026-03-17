package com.estapar.parking_management.garage.infrastructure.client.dto;

import java.math.BigDecimal;

/**
 * DTO para representar um setor retornado pelo simulador.
 */
public record SectorResponse(
        String sector,
        BigDecimal basePrice,
        Integer maxCapacity,
        String openHour,
        String closeHour,
        Integer durationLimitMinutes
) {
}
