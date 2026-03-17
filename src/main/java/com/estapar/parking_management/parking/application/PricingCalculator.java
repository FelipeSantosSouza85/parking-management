package com.estapar.parking_management.parking.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Parking pricing calculation service.
 * Applies 30-minute tolerance, ceiling rounding of hours, and dynamic adjustment by occupancy.
 */
@Component
public class PricingCalculator {

    private static final Logger log = LoggerFactory.getLogger(PricingCalculator.class);

    private static final int TOLERANCE_MINUTES = 30;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculates chargeable hours considering 30-minute tolerance.
     * Up to 30 minutes = free (0 hours). Above that, rounds up.
     *
     * @param parkingDuration parking duration
     * @return number of chargeable hours (0 or more)
     */
    public int calculateChargeableHours(Duration parkingDuration) {
        if (parkingDuration == null) {
            throw new IllegalArgumentException("parkingDuration must not be null");
        }
        long totalMinutes = parkingDuration.toMinutes();
        int chargeableHours;
        if (totalMinutes <= TOLERANCE_MINUTES) {
            chargeableHours = 0;
        } else {
            chargeableHours = (int) Math.ceil((double) totalMinutes / MINUTES_PER_HOUR);
        }
        log.debug("[PARKING] - [PRICING] chargeableHours={} for duration={}min", chargeableHours, totalMinutes);
        return chargeableHours;
    }

    /**
     * Calculates the applied hourly price based on base price and adjustment rate.
     * Formula: hourlyPrice = basePrice * (1 + adjustmentRate)
     *
     * @param basePrice      base price per hour
     * @param adjustmentRate adjustment rate (e.g. -0.10, 0.00, 0.10, 0.25)
     * @return final hourly price
     */
    public BigDecimal calculateHourlyPrice(BigDecimal basePrice, BigDecimal adjustmentRate) {
        if (basePrice == null) {
            throw new IllegalArgumentException("basePrice must not be null");
        }
        if (adjustmentRate == null) {
            throw new IllegalArgumentException("adjustmentRate must not be null");
        }
        BigDecimal multiplier = BigDecimal.ONE.add(adjustmentRate);
        BigDecimal hourlyPrice = basePrice.multiply(multiplier).setScale(SCALE, ROUNDING_MODE);
        log.debug("[PARKING] - [PRICING] hourlyPrice={} basePrice={} adjustmentRate={}",
                hourlyPrice, basePrice, adjustmentRate);
        return hourlyPrice;
    }

    /**
     * Calculates the total charged amount.
     * Formula: chargedAmount = hourlyPrice * chargeableHours
     *
     * @param hourlyPrice     applied hourly price
     * @param chargeableHours number of chargeable hours
     * @return total charged amount (ZERO if hours = 0)
     */
    public BigDecimal calculateChargedAmount(BigDecimal hourlyPrice, int chargeableHours) {
        if (hourlyPrice == null) {
            throw new IllegalArgumentException("hourlyPrice must not be null");
        }
        if (chargeableHours < 0) {
            throw new IllegalArgumentException("chargeableHours must not be negative, got: " + chargeableHours);
        }
        BigDecimal chargedAmount;
        if (chargeableHours == 0) {
            chargedAmount = BigDecimal.ZERO;
        } else {
            chargedAmount = hourlyPrice.multiply(BigDecimal.valueOf(chargeableHours)).setScale(SCALE, ROUNDING_MODE);
        }
        log.debug("[PARKING] - [PRICING] chargedAmount={} hourlyPrice={} chargeableHours={}",
                chargedAmount, hourlyPrice, chargeableHours);
        return chargedAmount;
    }
}
