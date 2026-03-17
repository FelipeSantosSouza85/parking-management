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
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create unoccupied spot by default")
        void shouldCreateSpotUnoccupiedByDefault() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            assertThat(spot.getExternalSpotId()).isEqualTo(1);
            assertThat(spot.getSector()).isSameAs(sector);
            assertThat(spot.getLat()).isEqualTo(-23.55);
            assertThat(spot.getLng()).isEqualTo(-46.63);
            assertThat(spot.isOccupied()).isFalse();
        }

        @Test
        @DisplayName("should reject null externalSpotId")
        void shouldRejectNullExternalSpotId() {
            assertThatThrownBy(() -> new ParkingSpot(null, sector, -23.55, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("externalSpotId");
        }

        @Test
        @DisplayName("should reject null sector")
        void shouldRejectNullSector() {
            assertThatThrownBy(() -> new ParkingSpot(1, null, -23.55, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sector");
        }

        @Test
        @DisplayName("should reject null lat")
        void shouldRejectNullLat() {
            assertThatThrownBy(() -> new ParkingSpot(1, sector, null, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("lat");
        }

        @Test
        @DisplayName("should reject null lng")
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
        @DisplayName("should update all fields preserving id and externalSpotId")
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
        @DisplayName("should reject null sector in updateFrom")
        void shouldRejectNullSectorOnUpdate() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            assertThatThrownBy(() -> spot.updateFrom(null, -23.55, -46.63, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("sector");
        }

        @Test
        @DisplayName("should update occupied to false via updateFrom")
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
        @DisplayName("should occupy free spot successfully")
        void shouldOccupyFreeSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            spot.occupy();

            assertThat(spot.isOccupied()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when occupying already occupied spot")
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
        @DisplayName("should release occupied spot successfully")
        void shouldReleasedOccupiedSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);
            spot.occupy();

            spot.release();

            assertThat(spot.isOccupied()).isFalse();
        }

        @Test
        @DisplayName("should throw exception when releasing free spot")
        void shouldThrowWhenReleasingFreeSpot() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            assertThatThrownBy(spot::release)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not occupied");
        }
    }

    @Nested
    @DisplayName("Full occupy/release cycle")
    class FullCycle {

        @Test
        @DisplayName("should allow occupy, release and occupy again")
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
