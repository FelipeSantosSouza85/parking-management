package com.estapar.parking_management.parking.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ParkingSession")
class ParkingSessionTest {

    @Nested
    @DisplayName("Criação")
    class Creation {

        @Test
        @DisplayName("deve criar sessão com campos obrigatórios")
        void shouldCreateSessionWithRequiredFields() {
            String licensePlate = "ABC1234";
            Instant entryTime = Instant.parse("2025-01-01T12:00:00Z");
            ParkingSessionStatus status = ParkingSessionStatus.ENTERED;

            ParkingSession session = new ParkingSession(licensePlate, entryTime, status);

            assertThat(session.getLicensePlate()).isEqualTo("ABC1234");
            assertThat(session.getEntryTime()).isEqualTo(entryTime);
            assertThat(session.getStatus()).isEqualTo(ParkingSessionStatus.ENTERED);
            assertThat(session.getId()).isNull();
            assertThat(session.getParkedTime()).isNull();
            assertThat(session.getExitTime()).isNull();
            assertThat(session.getParkingSpot()).isNull();
            assertThat(session.getSector()).isNull();
            assertThat(session.getOccupancyRateAtEntry()).isNull();
            assertThat(session.getPriceAdjustmentRateAtEntry()).isNull();
            assertThat(session.getHourlyPriceApplied()).isNull();
            assertThat(session.getChargedAmount()).isNull();
        }

        @Test
        @DisplayName("deve rejeitar licensePlate nulo")
        void shouldRejectNullLicensePlate() {
            assertThatThrownBy(() -> new ParkingSession(null, Instant.now(), ParkingSessionStatus.ENTERED))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("deve rejeitar licensePlate vazio")
        void shouldRejectBlankLicensePlate() {
            assertThatThrownBy(() -> new ParkingSession("", Instant.now(), ParkingSessionStatus.ENTERED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("licensePlate must not be blank");

            assertThatThrownBy(() -> new ParkingSession("   ", Instant.now(), ParkingSessionStatus.ENTERED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("licensePlate must not be blank");
        }

        @Test
        @DisplayName("deve rejeitar entryTime nulo")
        void shouldRejectNullEntryTime() {
            assertThatThrownBy(() -> new ParkingSession("ABC1234", null, ParkingSessionStatus.ENTERED))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("entryTime");
        }

        @Test
        @DisplayName("deve rejeitar status nulo")
        void shouldRejectNullStatus() {
            assertThatThrownBy(() -> new ParkingSession("ABC1234", Instant.now(), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("status");
        }
    }

    @Nested
    @DisplayName("Estado inicial")
    class InitialState {

        @Test
        @DisplayName("campos opcionais devem ser nulos após criação")
        void optionalFieldsShouldBeNullAfterCreation() {
            ParkingSession session = new ParkingSession("ZUL0001", Instant.now(), ParkingSessionStatus.ENTERED);

            assertThat(session.getParkedTime()).isNull();
            assertThat(session.getExitTime()).isNull();
            assertThat(session.getParkingSpot()).isNull();
            assertThat(session.getSector()).isNull();
            assertThat(session.getOccupancyRateAtEntry()).isNull();
            assertThat(session.getPriceAdjustmentRateAtEntry()).isNull();
            assertThat(session.getHourlyPriceApplied()).isNull();
            assertThat(session.getChargedAmount()).isNull();
        }

        @Test
        @DisplayName("deve aceitar todos os status no construtor")
        void shouldAcceptAllStatusesInConstructor() {
            Instant entryTime = Instant.now();

            for (ParkingSessionStatus status : ParkingSessionStatus.values()) {
                ParkingSession session = new ParkingSession("ABC1234", entryTime, status);
                assertThat(session.getStatus()).isEqualTo(status);
            }
        }
    }
}
