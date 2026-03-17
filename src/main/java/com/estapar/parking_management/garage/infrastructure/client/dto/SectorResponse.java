package com.estapar.parking_management.garage.infrastructure.client.dto;

import java.math.BigDecimal;

/**
 * DTO representing a sector returned by the simulator.
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
