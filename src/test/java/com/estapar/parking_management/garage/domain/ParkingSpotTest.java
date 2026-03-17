package com.estapar.parking_management.garage.domain;

import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ParkingSpot")
class ParkingSpotTest {

    private GarageSector sector;

    @BeforeEach
    void setUp() {
        sector = new GarageSector("A", new BigDecimal("10.00"), 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);
    }

    @Nested
    @DisplayName("Criação")
    class Creation {

        @Test
        @DisplayName("deve criar spot desocupado por padrão")
        void shouldCreateSpotUnoccupiedByDefault() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            assertThat(spot.getExternalSpotId()).isEqualTo(1);
            assertThat(spot.getSector()).isSameAs(sector);
            assertThat(spot.getLat()).isEqualTo(-23.55);
            assertThat(spot.getLng()).isEqualTo(-46.63);
            assertThat(spot.isOccupied()).isFalse();
        }

        @Test
        @DisplayName("deve rejeitar externalSpotId nulo")
        void shouldRejectNullExternalSpotId() {
            assertThatThrownBy(() -> new ParkingSpot(null, sector, -23.55, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("externalSpotId");
        }

        @Test
        @DisplayName("deve rejeitar sector nulo")
        void shouldRejectNullSector() {
            assertThatThrownBy(() -> new ParkingSpot(1, null, -23.55, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sector");
        }

        @Test
        @DisplayName("deve rejeitar lat nulo")
        void shouldRejectNullLat() {
            assertThatThrownBy(() -> new ParkingSpot(1, sector, null, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("lat");
        }

        @Test
        @DisplayName("deve rejeitar lng nulo")
        void shouldRejectNullLng() {
            assertThatThrownBy(() -> new ParkingSpot(1, sector, -23.55, null, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("lng");
        }
    }

    @Nested
    @DisplayName("updateFrom")
    class UpdateFrom {

        @Test
        @DisplayName("deve atualizar todos os campos mantendo id e externalSpotId")
        void shouldUpdateAllFieldsPreservingIdentity() {
            // Given
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);
            spot.setId(42L);

            GarageSector newSector = new GarageSector("B", new BigDecimal("20.00"), 50, LocalTime.of(9, 0), LocalTime.of(21, 0), 90);

            // When
            spot.updateFrom(newSector, -23.99, -46.99, true);

            // Then
            assertThat(spot.getId()).isEqualTo(42L);
            assertThat(spot.getExternalSpotId()).isEqualTo(1);
            assertThat(spot.getSector()).isSameAs(newSector);
            assertThat(spot.getLat()).isEqualTo(-23.99);
            assertThat(spot.getLng()).isEqualTo(-46.99);
            assertThat(spot.isOccupied()).isTrue();
        }

        @Test
        @DisplayName("deve rejeitar sector nulo no updateFrom")
        void shouldRejectNullSectorOnUpdate() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            assertThatThrownBy(() -> spot.updateFrom(null, -23.55, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sector");
        }

        @Test
        @DisplayName("deve atualizar occupied para false via updateFrom")
        void shouldUpdateOccupiedToFalse() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);
            spot.occupy();

            spot.updateFrom(sector, -23.55, -46.63, false);

            assertThat(spot.isOccupied()).isFalse();
        }
    }

    @Nested
    @DisplayName("occupy()")
    class Occupy {

        @Test
        @DisplayName("deve ocupar spot livre com sucesso")
        void shouldOccupyFreeSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            spot.occupy();

            assertThat(spot.isOccupied()).isTrue();
        }

        @Test
        @DisplayName("deve lançar exceção ao ocupar spot já ocupado")
        void shouldThrowWhenOccupyingAlreadyOccupiedSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);
            spot.occupy();

            assertThatThrownBy(spot::occupy)
                    .isInstanceOf(SpotAlreadyOccupiedException.class)
                    .hasMessageContaining("already occupied");
        }
    }

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("deve liberar spot ocupado com sucesso")
        void shouldReleasedOccupiedSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);
            spot.occupy();

            spot.release();

            assertThat(spot.isOccupied()).isFalse();
        }

        @Test
        @DisplayName("deve lançar exceção ao liberar spot já livre")
        void shouldThrowWhenReleasingFreeSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            assertThatThrownBy(spot::release)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not occupied");
        }
    }

    @Nested
    @DisplayName("Ciclo completo occupy/release")
    class FullCycle {

        @Test
        @DisplayName("deve permitir ocupar, liberar e ocupar novamente")
        void shouldAllowOccupyReleaseCycle() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            spot.occupy();
            assertThat(spot.isOccupied()).isTrue();

            spot.release();
            assertThat(spot.isOccupied()).isFalse();

            spot.occupy();
            assertThat(spot.isOccupied()).isTrue();
        }
    }
}
