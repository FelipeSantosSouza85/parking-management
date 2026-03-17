package com.estapar.parking_management.garage.infrastructure.persistence;

import com.estapar.parking_management.TestcontainersConfiguration;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    com.estapar.parking_management.garage.infrastructure.persistence.repository.adapter.GarageSectorAdapter.class,
    com.estapar.parking_management.garage.infrastructure.persistence.repository.adapter.ParkingSpotAdapter.class,
    com.estapar.parking_management.garage.infrastructure.persistence.mapper.GarageSectorMapper.class,
    com.estapar.parking_management.garage.infrastructure.persistence.mapper.ParkingSpotMapper.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ParkingSpotAdapter (via ParkingSpotPort)")
class ParkingSpotRepositoryTest {

    @Autowired
    ParkingSpotPort parkingSpotPort;

    @Autowired
    GarageSectorPort garageSectorPort;

    private GarageSector sector;

    @BeforeEach
    void setUp() {
        sector = garageSectorPort.save(
                new GarageSector("A", new BigDecimal("10.00"), 100, LocalTime.of(8, 0), LocalTime.of(22, 0), 120)
        );
    }

    @Nested
    @DisplayName("CRUD básico")
    class CrudOperations {

        @Test
        @DisplayName("deve persistir e recuperar spot por ID")
        void shouldPersistAndRetrieveById() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);

            ParkingSpot saved = parkingSpotPort.save(spot);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.isOccupied()).isFalse();

            Optional<ParkingSpot> found = parkingSpotPort.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getExternalSpotId()).isEqualTo(1);
            assertThat(found.get().getLat()).isEqualTo(-23.55);
            assertThat(found.get().getLng()).isEqualTo(-46.63);
        }
    }

    @Nested
    @DisplayName("findByLatAndLngWithLock")
    class FindByLatAndLng {

        @Test
        @DisplayName("deve encontrar spot por coordenadas")
        void shouldFindByLatAndLng() {
            parkingSpotPort.save(new ParkingSpot(1, sector, -23.55, -46.63, false));

            Optional<ParkingSpot> result = parkingSpotPort.findByLatAndLngWithLock(-23.55, -46.63);

            assertThat(result).isPresent();
            assertThat(result.get().getExternalSpotId()).isEqualTo(1);
        }

        @Test
        @DisplayName("deve retornar vazio para coordenadas inexistentes")
        void shouldReturnEmptyForNonExistentCoordinates() {
            Optional<ParkingSpot> result = parkingSpotPort.findByLatAndLngWithLock(0.0, 0.0);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByExternalSpotId")
    class FindByExternalSpotId {

        @Test
        @DisplayName("deve encontrar spot por externalSpotId")
        void shouldFindByExternalSpotId() {
            parkingSpotPort.save(new ParkingSpot(42, sector, -23.55, -46.63, false));

            Optional<ParkingSpot> result = parkingSpotPort.findByExternalSpotId(42);

            assertThat(result).isPresent();
            assertThat(result.get().getLat()).isEqualTo(-23.55);
        }

        @Test
        @DisplayName("deve retornar vazio para externalSpotId inexistente")
        void shouldReturnEmptyForNonExistentExternalSpotId() {
            Optional<ParkingSpot> result = parkingSpotPort.findByExternalSpotId(999);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unique constraints")
    class UniqueConstraints {

        @Test
        @DisplayName("deve rejeitar external_spot_id duplicado")
        void shouldRejectDuplicateExternalSpotId() {
            parkingSpotPort.save(new ParkingSpot(1, sector, -23.55, -46.63, false));

            assertThatThrownBy(() -> parkingSpotPort.save(new ParkingSpot(1, sector, -23.56, -46.64, false)))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("deve rejeitar coordenadas (lat, lng) duplicadas")
        void shouldRejectDuplicateCoordinates() {
            parkingSpotPort.save(new ParkingSpot(1, sector, -23.55, -46.63, false));

            assertThatThrownBy(() -> parkingSpotPort.save(new ParkingSpot(2, sector, -23.55, -46.63, false)))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Estado de ocupação persistido")
    class OccupiedState {

        @Test
        @DisplayName("deve persistir estado de ocupação após occupy()")
        void shouldPersistOccupiedState() {
            ParkingSpot spot = new ParkingSpot(1, sector, -23.55, -46.63, false);
            spot.occupy();
            ParkingSpot saved = parkingSpotPort.save(spot);

            Optional<ParkingSpot> found = parkingSpotPort.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().isOccupied()).isTrue();
        }
    }
}
