package com.estapar.parking_management.garage.infrastructure.persistence;

import com.estapar.parking_management.TestcontainersConfiguration;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.domain.GarageSector;
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
    com.estapar.parking_management.garage.infrastructure.persistence.mapper.GarageSectorMapper.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("GarageSectorAdapter (via GarageSectorPort)")
class GarageSectorRepositoryTest {

    @Autowired
    GarageSectorPort port;

    private GarageSector createSector(String code) {
        return new GarageSector(code, new BigDecimal("15.50"), 50, LocalTime.of(8, 0), LocalTime.of(22, 0), 120);
    }

    @Nested
    @DisplayName("CRUD básico")
    class CrudOperations {

        @Test
        @DisplayName("deve persistir e recuperar setor por ID")
        void shouldPersistAndRetrieveById() {
            GarageSector sector = createSector("A");

            GarageSector saved = port.save(sector);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();

            Optional<GarageSector> found = port.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getSectorCode()).isEqualTo("A");
            assertThat(found.get().getBasePrice()).isEqualByComparingTo("15.50");
        }

        @Test
        @DisplayName("deve listar todos os setores")
        void shouldFindAll() {
            port.save(createSector("A"));
            port.save(createSector("B"));

            assertThat(port.findAll()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findBySectorCode")
    class FindBySectorCode {

        @Test
        @DisplayName("deve encontrar setor por código existente")
        void shouldFindBySectorCode() {
            port.save(createSector("A"));

            Optional<GarageSector> result = port.findBySectorCode("A");

            assertThat(result).isPresent();
            assertThat(result.get().getSectorCode()).isEqualTo("A");
        }

        @Test
        @DisplayName("deve retornar vazio para código inexistente")
        void shouldReturnEmptyForNonExistentCode() {
            Optional<GarageSector> result = port.findBySectorCode("Z");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unique constraints")
    class UniqueConstraints {

        @Test
        @DisplayName("deve rejeitar sector_code duplicado")
        void shouldRejectDuplicateSectorCode() {
            port.save(createSector("A"));

            assertThatThrownBy(() -> port.save(createSector("A")))
                    .isInstanceOf(Exception.class);
        }
    }
}
