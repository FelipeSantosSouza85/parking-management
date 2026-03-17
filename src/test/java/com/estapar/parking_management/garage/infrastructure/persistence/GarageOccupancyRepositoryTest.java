package com.estapar.parking_management.garage.infrastructure.persistence;

import com.estapar.parking_management.TestcontainersConfiguration;
import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    com.estapar.parking_management.garage.infrastructure.persistence.repository.adapter.GarageOccupancyAdapter.class,
    com.estapar.parking_management.garage.infrastructure.persistence.mapper.GarageOccupancyMapper.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("GarageOccupancyAdapter (via GarageOccupancyPort)")
class GarageOccupancyRepositoryTest {

    @Autowired
    GarageOccupancyPort port;

    @Nested
    @DisplayName("CRUD básico")
    class CrudOperations {

        @Test
        @DisplayName("deve persistir e recuperar occupancy por ID")
        void shouldPersistAndRetrieveById() {
            GarageOccupancy occupancy = new GarageOccupancy(200);

            GarageOccupancy saved = port.save(occupancy);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTotalCapacity()).isEqualTo(200);
            assertThat(saved.getOccupiedCount()).isZero();
        }
    }

    @Nested
    @DisplayName("findByIdWithLock")
    class FindByIdWithLock {

        @Test
        @DisplayName("deve recuperar occupancy com lock pessimista")
        void shouldRetrieveWithPessimisticLock() {
            GarageOccupancy saved = port.save(new GarageOccupancy(100));

            Optional<GarageOccupancy> result = port.findByIdWithLock(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getTotalCapacity()).isEqualTo(100);
        }

        @Test
        @DisplayName("deve retornar vazio para ID inexistente")
        void shouldReturnEmptyForNonExistentId() {
            Optional<GarageOccupancy> result = port.findByIdWithLock(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Persistência de estado")
    class StatePersistence {

        @Test
        @DisplayName("deve persistir occupiedCount após incremento")
        void shouldPersistOccupiedCountAfterIncrement() {
            GarageOccupancy occupancy = new GarageOccupancy(100);
            occupancy.incrementOccupied();
            occupancy.incrementOccupied();
            GarageOccupancy saved = port.save(occupancy);

            Optional<GarageOccupancy> found = port.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getOccupiedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("deve persistir occupiedCount após decremento")
        void shouldPersistOccupiedCountAfterDecrement() {
            GarageOccupancy occupancy = new GarageOccupancy(100);
            occupancy.incrementOccupied();
            occupancy.incrementOccupied();
            occupancy.decrementOccupied();
            GarageOccupancy saved = port.save(occupancy);

            Optional<GarageOccupancy> found = port.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getOccupiedCount()).isEqualTo(1);
        }
    }
}
