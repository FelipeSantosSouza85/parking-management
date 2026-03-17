package com.estapar.parking_management.parking.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Dynamic price adjustment policy based on parking occupancy rate.
 * Stateless singleton that applies the adjustment table by occupancy range.
 */
@Component
public class PricingAdjustmentPolicy {

    private static final Logger log = LoggerFactory.getLogger(PricingAdjustmentPolicy.class);

    private static final BigDecimal ADJUSTMENT_LOW = new BigDecimal("-0.10");   // < 25%
    private static final BigDecimal ADJUSTMENT_NORMAL = BigDecimal.ZERO;        // 25%-50% (até 50% inclusive)
    private static final BigDecimal ADJUSTMENT_HIGH = new BigDecimal("0.10");  // > 50% até 75%
    private static final BigDecimal ADJUSTMENT_PEAK = new BigDecimal("0.25");  // > 75% até 100%

    private static final BigDecimal THRESHOLD_25 = new BigDecimal("0.25");
    private static final BigDecimal THRESHOLD_50 = new BigDecimal("0.50");
    private static final BigDecimal THRESHOLD_75 = new BigDecimal("0.75");

    /**
     * Returns the adjustment rate corresponding to the given occupancy rate.
     *
     * @param occupancyRate occupancy rate
     * @return adjustment rate: -0.10 (<25%), 0.00 [25%-50%], +0.10 (>= 50%-75%], +0.25 (>= 75%-100%]
     * @throws IllegalArgumentException if occupancyRate is null, negative, or greater than 1
     */
    public BigDecimal getAdjustmentRate(BigDecimal occupancyRate) {
        validateOccupancyRate(occupancyRate);

        BigDecimal rate;
        if (occupancyRate.compareTo(THRESHOLD_25) < 0) {
            rate = ADJUSTMENT_LOW;
        } else if (occupancyRate.compareTo(THRESHOLD_50) <= 0) {
            rate = ADJUSTMENT_NORMAL;
        } else if (occupancyRate.compareTo(THRESHOLD_75) <= 0) {
            rate = ADJUSTMENT_HIGH;
        } else {
            rate = ADJUSTMENT_PEAK;
        }

        log.debug("[PARKING] - [PRICING] occupancyRate={}, adjustmentRate={}", occupancyRate, rate);
        return rate;
    }

    private void validateOccupancyRate(BigDecimal occupancyRate) {
        if (occupancyRate == null) {
            throw new IllegalArgumentException("occupancyRate must not be null");
        }
        if (occupancyRate.compareTo(BigDecimal.ZERO) < 0 || occupancyRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("occupancyRate must be between 0 and 1 (inclusive), got: " + occupancyRate);
        }
    }
}
