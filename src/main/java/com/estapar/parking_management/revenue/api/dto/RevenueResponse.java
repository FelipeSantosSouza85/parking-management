package com.estapar.parking_management.revenue.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de resposta da API de receita por setor e data.
 */
public record RevenueResponse(BigDecimal amount, String currency, Instant timestamp) {
}
