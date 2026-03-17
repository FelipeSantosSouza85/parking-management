package com.estapar.parking_management.garage.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GarageSector")
class GarageSectorTest {

    @Nested
    @DisplayName("Criação")
    class Creation {

        @Test
        @DisplayName("deve criar setor com todos os campos obrigatórios")
        void shouldCreateSectorWithAllRequiredFields() {
            // Given
            String sectorCode = "A";
            BigDecimal basePrice = new BigDecimal("10.00");
            Integer maxCapacity = 100;
            LocalTime openHour = LocalTime.of(8, 0);
            LocalTime closeHour = LocalTime.of(22, 0);
            Integer durationLimitMinutes = 120;

            // When
            GarageSector sector = new GarageSector(sectorCode, basePrice, maxCapacity, openHour, closeHour, durationLimitMinutes);

            // Then
            assertThat(sector.getSectorCode()).isEqualTo("A");
            assertThat(sector.getBasePrice()).isEqualByComparingTo("10.00");
            assertThat(sector.getMaxCapacity()).isEqualTo(100);
            assertThat(sector.getOpenHour()).isEqualTo(LocalTime.of(8, 0));
            assertThat(sector.getCloseHour()).isEqualTo(LocalTime.of(22, 0));
            assertThat(sector.getDurationLimitMinutes()).isEqualTo(120);
            assertThat(sector.getId()).isNull();
        }

        @Test
        @DisplayName("deve rejeitar sectorCode nulo")
        void shouldRejectNullSectorCode() {
            assertThatThrownBy(() -> new GarageSector(null, BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sectorCode");
        }

        @Test
        @DisplayName("deve rejeitar basePrice nulo")
        void shouldRejectNullBasePrice() {
            assertThatThrownBy(() -> new GarageSector("A", null, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("basePrice");
        }

        @Test
        @DisplayName("deve rejeitar maxCapacity nulo")
        void shouldRejectNullMaxCapacity() {
            assertThatThrownBy(() -> new GarageSector("A", BigDecimal.TEN, null, LocalTime.of(8, 0), LocalTime.of(22, 0), 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("maxCapacity");
        }

        @Test
        @DisplayName("deve rejeitar openHour nulo")
        void shouldRejectNullOpenHour() {
            assertThatThrownBy(() -> new GarageSector("A", BigDecimal.TEN, 100, null, LocalTime.of(22, 0), 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("openHour");
        }

        @Test
        @DisplayName("deve rejeitar closeHour nulo")
        void shouldRejectNullCloseHour() {
            assertThatThrownBy(() -> new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), null, 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("closeHour");
        }

        @Test
        @DisplayName("deve rejeitar durationLimitMinutes nulo")
        void shouldRejectNullDurationLimitMinutes() {
            assertThatThrownBy(() -> new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("durationLimitMinutes");
        }
    }

    @Nested
    @DisplayName("updateFrom")
    class UpdateFrom {

        @Test
        @DisplayName("deve atualizar todos os campos mantendo id e createdAt")
        void shouldUpdateAllFieldsPreservingIdentity() {
            // Given
            GarageSector sector = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);
            sector.setId(42L);

            // When
            sector.updateFrom("B", new BigDecimal("25.00"), 50, LocalTime.of(9, 0), LocalTime.of(21, 0), 90);

            // Then
            assertThat(sector.getId()).isEqualTo(42L);
            assertThat(sector.getSectorCode()).isEqualTo("B");
            assertThat(sector.getBasePrice()).isEqualByComparingTo("25.00");
            assertThat(sector.getMaxCapacity()).isEqualTo(50);
            assertThat(sector.getOpenHour()).isEqualTo(LocalTime.of(9, 0));
            assertThat(sector.getCloseHour()).isEqualTo(LocalTime.of(21, 0));
            assertThat(sector.getDurationLimitMinutes()).isEqualTo(90);
        }

        @Test
        @DisplayName("deve rejeitar sectorCode nulo no updateFrom")
        void shouldRejectNullSectorCodeOnUpdate() {
            GarageSector sector = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);

            assertThatThrownBy(() -> sector.updateFrom(null, BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sectorCode");
        }

        @Test
        @DisplayName("deve rejeitar basePrice nulo no updateFrom")
        void shouldRejectNullBasePriceOnUpdate() {
            GarageSector sector = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);

            assertThatThrownBy(() -> sector.updateFrom("A", null, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("basePrice");
        }
    }

    @Nested
    @DisplayName("Igualdade e hashCode")
    class EqualityAndHashCode {

        @Test
        @DisplayName("dois setores sem ID não devem ser iguais")
        void twoSectorsWithoutIdShouldNotBeEqual() {
            GarageSector sector1 = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);
            GarageSector sector2 = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);

            assertThat(sector1).isNotEqualTo(sector2);
        }

        @Test
        @DisplayName("setor deve ser igual a si mesmo")
        void sectorShouldBeEqualToItself() {
            GarageSector sector = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);

            assertThat(sector).isEqualTo(sector);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("deve incluir sectorCode no toString")
        void shouldIncludeSectorCodeInToString() {
            GarageSector sector = new GarageSector("A", BigDecimal.TEN, 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);

            assertThat(sector.toString()).contains("sectorCode='A'");
        }
    }
}
