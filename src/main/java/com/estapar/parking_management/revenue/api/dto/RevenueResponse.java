package com.estapar.parking_management.revenue.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for the revenue API by sector and date.
 */
public record RevenueResponse(BigDecimal amount, String currency, Instant timestamp) {
}
