package com.estapar.parking_management.garage.domain;

import com.estapar.parking_management.shared.exception.GarageFullException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GarageOccupancy")
class GarageOccupancyTest {

    @Nested
    @DisplayName("Criação")
    class Creation {

        @Test
        @DisplayName("deve criar occupancy com contador zerado")
        void shouldCreateOccupancyWithZeroCount() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            assertThat(occupancy.getTotalCapacity()).isEqualTo(100);
            assertThat(occupancy.getOccupiedCount()).isZero();
        }

        @Test
        @DisplayName("deve rejeitar totalCapacity nulo")
        void shouldRejectNullTotalCapacity() {
            assertThatThrownBy(() -> new GarageOccupancy(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive number");
        }

        @Test
        @DisplayName("deve rejeitar totalCapacity zero")
        void shouldRejectZeroTotalCapacity() {
            assertThatThrownBy(() -> new GarageOccupancy(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive number");
        }

        @Test
        @DisplayName("deve rejeitar totalCapacity negativo")
        void shouldRejectNegativeTotalCapacity() {
            assertThatThrownBy(() -> new GarageOccupancy(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive number");
        }
    }

    @Nested
    @DisplayName("incrementOccupied()")
    class IncrementOccupied {

        @Test
        @DisplayName("deve incrementar contador de ocupação")
        void shouldIncrementOccupiedCount() {
            GarageOccupancy occupancy = new GarageOccupancy(10);

            occupancy.incrementOccupied();

            assertThat(occupancy.getOccupiedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve permitir múltiplos incrementos até capacidade máxima")
        void shouldAllowMultipleIncrementsUpToCapacity() {
            GarageOccupancy occupancy = new GarageOccupancy(3);

            occupancy.incrementOccupied();
            occupancy.incrementOccupied();
            occupancy.incrementOccupied();

            assertThat(occupancy.getOccupiedCount()).isEqualTo(3);
            assertThat(occupancy.isFull()).isTrue();
        }

        @Test
        @DisplayName("deve lançar exceção ao incrementar quando cheio")
        void shouldThrowWhenIncrementingFullGarage() {
            GarageOccupancy occupancy = new GarageOccupancy(1);
            occupancy.incrementOccupied();

            assertThatThrownBy(occupancy::incrementOccupied)
                    .isInstanceOf(GarageFullException.class)
                    .hasMessageContaining("full");
        }
    }

    @Nested
    @DisplayName("decrementOccupied()")
    class DecrementOccupied {

        @Test
        @DisplayName("deve decrementar contador de ocupação")
        void shouldDecrementOccupiedCount() {
            GarageOccupancy occupancy = new GarageOccupancy(10);
            occupancy.incrementOccupied();

            occupancy.decrementOccupied();

            assertThat(occupancy.getOccupiedCount()).isZero();
        }

        @Test
        @DisplayName("deve lançar exceção ao decrementar quando zerado")
        void shouldThrowWhenDecrementingZeroCount() {
            GarageOccupancy occupancy = new GarageOccupancy(10);

            assertThatThrownBy(occupancy::decrementOccupied)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already zero");
        }
    }

    @Nested
    @DisplayName("getOccupancyRate()")
    class OccupancyRate {

        @Test
        @DisplayName("deve retornar 0 quando vazio")
        void shouldReturnZeroWhenEmpty() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            assertThat(occupancy.getOccupancyRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("deve retornar 1.0000 quando cheio")
        void shouldReturnOneWhenFull() {
            GarageOccupancy occupancy = new GarageOccupancy(2);
            occupancy.incrementOccupied();
            occupancy.incrementOccupied();

            assertThat(occupancy.getOccupancyRate()).isEqualByComparingTo(new BigDecimal("1.0000"));
        }

        @Test
        @DisplayName("deve retornar taxa correta com 4 casas decimais")
        void shouldReturnCorrectRateWithFourDecimals() {
            GarageOccupancy occupancy = new GarageOccupancy(3);
            occupancy.incrementOccupied();

            assertThat(occupancy.getOccupancyRate()).isEqualByComparingTo(new BigDecimal("0.3333"));
        }
    }

    @Nested
    @DisplayName("isFull()")
    class IsFull {

        @Test
        @DisplayName("deve retornar false quando não está cheio")
        void shouldReturnFalseWhenNotFull() {
            GarageOccupancy occupancy = new GarageOccupancy(10);

            assertThat(occupancy.isFull()).isFalse();
        }

        @Test
        @DisplayName("deve retornar true quando está cheio")
        void shouldReturnTrueWhenFull() {
            GarageOccupancy occupancy = new GarageOccupancy(1);
            occupancy.incrementOccupied();

            assertThat(occupancy.isFull()).isTrue();
        }
    }

    @Nested
    @DisplayName("Ciclo completo increment/decrement")
    class FullCycle {

        @Test
        @DisplayName("deve permitir incrementar, decrementar e incrementar novamente")
        void shouldAllowIncrementDecrementCycle() {
            GarageOccupancy occupancy = new GarageOccupancy(1);

            occupancy.incrementOccupied();
            assertThat(occupancy.isFull()).isTrue();

            occupancy.decrementOccupied();
            assertThat(occupancy.isFull()).isFalse();
            assertThat(occupancy.getOccupiedCount()).isZero();

            occupancy.incrementOccupied();
            assertThat(occupancy.isFull()).isTrue();
        }
    }
}
