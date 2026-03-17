package com.estapar.parking_management.parking.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Serviço de cálculo de precificação de estacionamento.
 * Aplica tolerância de 30 minutos, arredondamento para cima de horas e ajuste dinâmico por ocupação.
 */
@Component
public class PricingCalculator {

    private static final Logger log = LoggerFactory.getLogger(PricingCalculator.class);

    private static final int TOLERANCE_MINUTES = 30;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calcula as horas cobráveis considerando tolerância de 30 minutos.
     * Até 30 minutos = gratuito (0 horas). Acima disso, arredonda para cima.
     *
     * @param parkingDuration duração do estacionamento
     * @return número de horas cobráveis (0 ou mais)
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
     * Calcula o preço por hora aplicado com base no preço base e taxa de ajuste.
     * Fórmula: hourlyPrice = basePrice * (1 + adjustmentRate)
     *
     * @param basePrice      preço base por hora
     * @param adjustmentRate taxa de ajuste (ex: -0.10, 0.00, 0.10, 0.25)
     * @return preço por hora final
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
     * Calcula o valor total cobrado.
     * Fórmula: chargedAmount = hourlyPrice * chargeableHours
     *
     * @param hourlyPrice     preço por hora aplicado
     * @param chargeableHours número de horas cobráveis
     * @return valor total cobrado (ZERO se horas = 0)
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
