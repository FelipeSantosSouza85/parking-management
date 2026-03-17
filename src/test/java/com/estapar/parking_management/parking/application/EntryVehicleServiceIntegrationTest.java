package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.TestcontainersConfiguration;
import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionAlreadyExistsException;
import com.estapar.parking_management.shared.exception.GarageFullException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("EntryVehicleService (integração)")
class EntryVehicleServiceIntegrationTest {

    private static final String LICENSE_PLATE = "ABC-1234";
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.ofInstant(Instant.parse("2025-03-13T10:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private EntryVehicleService entryVehicleService;

    @Autowired
    private GarageOccupancyPort garageOccupancyPort;

    @Autowired
    private ParkingSessionPort parkingSessionPort;

    @BeforeEach
    void setUp() {
        garageOccupancyPort.deleteAll();
        GarageOccupancy occupancy = new GarageOccupancy(100);
        garageOccupancyPort.save(occupancy);
    }

    @Nested
    @DisplayName("Entry com sucesso")
    class EntryComSucesso {

        @Test
        @DisplayName("verifica persistência real no banco")
        void verificaPersistenciaRealNoBanco() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            Optional<ParkingSession> result = parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE);
            assertThat(result).isPresent();
            ParkingSession session = result.get();
            assertThat(session.getId()).isNotNull();
            assertThat(session.getLicensePlate()).isEqualTo(LICENSE_PLATE);
            assertThat(session.getStatus()).isEqualTo(ParkingSessionStatus.ENTERED);
            assertThat(session.getEntryTime()).isEqualTo(ENTRY_TIME.toInstant(ZoneOffset.UTC));
            assertThat(session.getOccupancyRateAtEntry()).isNotNull();
            assertThat(session.getPriceAdjustmentRateAtEntry()).isNotNull();

            Optional<ParkingSession> found = parkingSessionPort.findById(session.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getLicensePlate()).isEqualTo(LICENSE_PLATE);
            assertThat(found.get().getStatus()).isEqualTo(ParkingSessionStatus.ENTERED);
        }
    }

    @Nested
    @DisplayName("Entry duplicada")
    class EntryDuplicada {

        @Test
        @DisplayName("verifica exceção com dados reais")
        void verificaExcecaoComDadosReais() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            assertThatThrownBy(() -> entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(ActiveSessionAlreadyExistsException.class)
                    .hasMessageContaining(LICENSE_PLATE);
        }
    }

    @Nested
    @DisplayName("Entry com garagem cheia")
    class EntryComGaragemCheia {

        @BeforeEach
        void setUpGaragemCheia() {
            garageOccupancyPort.deleteAll();
            GarageOccupancy occupancy = new GarageOccupancy(1);
            occupancy.incrementOccupied();
            garageOccupancyPort.save(occupancy);
        }

        @Test
        @DisplayName("verifica exceção com ocupação real no limite")
        void verificaExcecaoComOcupacaoRealNoLimite() {
            assertThatThrownBy(() -> entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(GarageFullException.class);
        }
    }

    @Nested
    @DisplayName("occupiedCount incrementado")
    class OccupiedCountIncrementado {

        @Test
        @DisplayName("verifica que occupiedCount é de fato incrementado no banco após entry")
        void verificaOccupiedCountIncrementadoNoBanco() {
            Optional<GarageOccupancy> before = garageOccupancyPort.findWithLock();
            assertThat(before).isPresent();
            assertThat(before.get().getOccupiedCount()).isZero();

            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            Optional<GarageOccupancy> after = garageOccupancyPort.findById(before.get().getId());
            assertThat(after).isPresent();
            assertThat(after.get().getOccupiedCount()).isEqualTo(1);
        }
    }
}
