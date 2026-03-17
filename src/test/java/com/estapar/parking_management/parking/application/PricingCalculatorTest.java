package com.estapar.parking_management.parking.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PricingCalculator")
class PricingCalculatorTest {

    private PricingCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PricingCalculator();
    }

    @Nested
    @DisplayName("calculateChargeableHours")
    class CalculateChargeableHours {

        @Test
        @DisplayName("duração 0 min retorna 0 horas")
        void duracao0MinRetorna0Horas() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(0))).isZero();
        }

        @Test
        @DisplayName("duração 30 min retorna 0 horas (limite tolerância)")
        void duracao30MinRetorna0Horas() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(30))).isZero();
        }

        @Test
        @DisplayName("duração 31 min retorna 1 hora")
        void duracao31MinRetorna1Hora() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(31))).isOne();
        }

        @Test
        @DisplayName("duração 60 min retorna 1 hora")
        void duracao60MinRetorna1Hora() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(60))).isOne();
        }

        @Test
        @DisplayName("duração 61 min retorna 2 horas")
        void duracao61MinRetorna2Horas() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(61))).isEqualTo(2);
        }

        @Test
        @DisplayName("duração 90 min retorna 2 horas")
        void duracao90MinRetorna2Horas() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(90))).isEqualTo(2);
        }

        @Test
        @DisplayName("duração 120 min retorna 2 horas")
        void duracao120MinRetorna2Horas() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(120))).isEqualTo(2);
        }

        @Test
        @DisplayName("duração 121 min retorna 3 horas")
        void duracao121MinRetorna3Horas() {
            assertThat(calculator.calculateChargeableHours(Duration.ofMinutes(121))).isEqualTo(3);
        }

        @Test
        @DisplayName("lança exceção para null")
        void lancaExcecaoParaNull() {
            assertThatThrownBy(() -> calculator.calculateChargeableHours(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }
    }

    @Nested
    @DisplayName("calculateHourlyPrice")
    class CalculateHourlyPrice {

        @Test
        @DisplayName("basePrice 40.00, ajuste -10% retorna 36.00")
        void base40AjusteMenos10Retorna36() {
            assertThat(calculator.calculateHourlyPrice(new BigDecimal("40.00"), new BigDecimal("-0.10")))
                    .isEqualByComparingTo("36.00");
        }

        @Test
        @DisplayName("basePrice 40.00, ajuste 0% retorna 40.00")
        void base40Ajuste0Retorna40() {
            assertThat(calculator.calculateHourlyPrice(new BigDecimal("40.00"), BigDecimal.ZERO))
                    .isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("basePrice 40.00, ajuste +10% retorna 44.00")
        void base40AjusteMais10Retorna44() {
            assertThat(calculator.calculateHourlyPrice(new BigDecimal("40.00"), new BigDecimal("0.10")))
                    .isEqualByComparingTo("44.00");
        }

        @Test
        @DisplayName("basePrice 40.00, ajuste +25% retorna 50.00")
        void base40AjusteMais25Retorna50() {
            assertThat(calculator.calculateHourlyPrice(new BigDecimal("40.00"), new BigDecimal("0.25")))
                    .isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("basePrice 40.50, ajuste 10% retorna 44.55 (precisão decimal)")
        void base40_50Ajuste10Retorna44_55() {
            assertThat(calculator.calculateHourlyPrice(new BigDecimal("40.50"), new BigDecimal("0.10")))
                    .isEqualByComparingTo("44.55");
        }

        @Test
        @DisplayName("lança exceção para basePrice null")
        void lancaExcecaoParaBasePriceNull() {
            assertThatThrownBy(() -> calculator.calculateHourlyPrice(null, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("basePrice");
        }

        @Test
        @DisplayName("lança exceção para adjustmentRate null")
        void lancaExcecaoParaAdjustmentRateNull() {
            assertThatThrownBy(() -> calculator.calculateHourlyPrice(new BigDecimal("40.00"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("adjustmentRate");
        }
    }

    @Nested
    @DisplayName("calculateChargedAmount")
    class CalculateChargedAmount {

        @Test
        @DisplayName("hourlyPrice 40.00, 0 horas retorna 0.00")
        void hourly40ZeroHorasRetorna0() {
            assertThat(calculator.calculateChargedAmount(new BigDecimal("40.00"), 0))
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("hourlyPrice 40.00, 1 hora retorna 40.00")
        void hourly40UmaHoraRetorna40() {
            assertThat(calculator.calculateChargedAmount(new BigDecimal("40.00"), 1))
                    .isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("hourlyPrice 40.00, 2 horas retorna 80.00")
        void hourly40DuasHorasRetorna80() {
            assertThat(calculator.calculateChargedAmount(new BigDecimal("40.00"), 2))
                    .isEqualByComparingTo("80.00");
        }

        @Test
        @DisplayName("hourlyPrice 44.55, 3 horas retorna 133.65")
        void hourly44_55TresHorasRetorna133_65() {
            assertThat(calculator.calculateChargedAmount(new BigDecimal("44.55"), 3))
                    .isEqualByComparingTo("133.65");
        }

        @Test
        @DisplayName("lança exceção para hourlyPrice null")
        void lancaExcecaoParaHourlyPriceNull() {
            assertThatThrownBy(() -> calculator.calculateChargedAmount(null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hourlyPrice");
        }

        @Test
        @DisplayName("lança exceção para chargeableHours negativo")
        void lancaExcecaoParaChargeableHoursNegativo() {
            assertThatThrownBy(() -> calculator.calculateChargedAmount(new BigDecimal("40.00"), -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("chargeableHours");
        }
    }
}
