package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.TestDataCleaner;
import com.estapar.parking_management.TestcontainersConfiguration;
import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("ExitVehicleService (integração)")
class ExitVehicleServiceIntegrationTest {

    private static final String LICENSE_PLATE = "ZUL0001";
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.ofInstant(Instant.parse("2025-03-13T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime EXIT_TIME = LocalDateTime.ofInstant(Instant.parse("2025-03-13T12:00:00Z"), ZoneOffset.UTC);
    private static final Double SPOT_LAT = -23.561684;
    private static final Double SPOT_LNG = -46.655981;

    @Autowired
    private ExitVehicleService exitVehicleService;

    @Autowired
    private EntryVehicleService entryVehicleService;

    @Autowired
    private ParkVehicleService parkVehicleService;

    @Autowired
    private GarageOccupancyPort garageOccupancyPort;

    @Autowired
    private GarageSectorPort garageSectorPort;

    @Autowired
    private ParkingSpotPort parkingSpotPort;

    @Autowired
    private ParkingSessionPort parkingSessionPort;

    @Autowired
    private TestDataCleaner testDataCleaner;

    private GarageOccupancy occupancy;
    private GarageSector sector;
    private ParkingSpot spot;

    @BeforeEach
    void setUp() {
        testDataCleaner.cleanAll();

        occupancy = new GarageOccupancy(100);
        occupancy.setOccupiedCount(30);
        occupancy = garageOccupancyPort.save(occupancy);

        sector = garageSectorPort.save(
                new GarageSector("A", new BigDecimal("10.00"), 100,
                        LocalTime.of(8, 0), LocalTime.of(22, 0), 120));

        spot = parkingSpotPort.save(new ParkingSpot(1, sector, SPOT_LAT, SPOT_LNG, false));
    }

    @Nested
    @DisplayName("EXIT com sucesso (PARKED -> EXITED)")
    class ExitComSucessoParked {

        @Test
        @DisplayName("Fluxo completo: ENTRY -> PARKED -> EXIT")
        void fluxoCompletoEntryParkedExit() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            Optional<ParkingSession> beforeExit = parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE);
            assertThat(beforeExit).isPresent();
            Long sessionId = beforeExit.get().getId();

            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            Optional<ParkingSession> result = parkingSessionPort.findById(sessionId);
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isNotNull();
            assertThat(result.get().getStatus()).isEqualTo(ParkingSessionStatus.EXITED);
            assertThat(result.get().getExitTime()).isEqualTo(EXIT_TIME.toInstant(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("Verifica persistência real: status EXITED, chargedAmount calculado, exitTime definido")
        void verificaPersistenciaRealStatusEChargedAmount() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            Optional<ParkingSession> beforeExit = parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE);
            assertThat(beforeExit).isPresent();
            Long sessionId = beforeExit.get().getId();

            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            Optional<ParkingSession> result = parkingSessionPort.findById(sessionId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(ParkingSessionStatus.EXITED);
            assertThat(result.get().getChargedAmount()).isNotNull();
            assertThat(result.get().getChargedAmount()).isEqualByComparingTo("20.00");
            assertThat(result.get().getExitTime()).isEqualTo(EXIT_TIME.toInstant(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("Verifica que o spot foi liberado no banco (occupied = false)")
        void verificaSpotFoiLiberadoNoBanco() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            Optional<ParkingSpot> foundSpot = parkingSpotPort.findById(spot.getId());
            assertThat(foundSpot).isPresent();
            assertThat(foundSpot.get().isOccupied()).isFalse();
        }

        @Test
        @DisplayName("Verifica que occupancy foi decrementada no banco")
        void verificaOccupancyDecrementadaNoBanco() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            int occupiedBefore = garageOccupancyPort.findWithLock()
                    .map(o -> o.getOccupiedCount())
                    .orElse(0);

            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            Optional<GarageOccupancy> occupancyAfter = garageOccupancyPort.findById(occupancy.getId());
            assertThat(occupancyAfter).isPresent();
            assertThat(occupancyAfter.get().getOccupiedCount()).isEqualTo(occupiedBefore - 1);
        }
    }

    @Nested
    @DisplayName("EXIT sem PARKED (ENTERED -> EXITED)")
    class ExitSemParked {

        @Test
        @DisplayName("Fluxo: ENTRY -> EXIT (sem PARKED)")
        void fluxoEntryExitSemParked() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            Optional<ParkingSession> beforeExit = parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE);
            assertThat(beforeExit).isPresent();
            Long sessionId = beforeExit.get().getId();

            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            Optional<ParkingSession> result = parkingSessionPort.findById(sessionId);
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo(ParkingSessionStatus.EXITED);
            assertThat(result.get().getChargedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.get().getParkingSpot()).isNull();
            assertThat(result.get().getSector()).isNull();
        }
    }

    @Nested
    @DisplayName("Sessão não encontrada")
    class SessaoNaoEncontrada {

        @Test
        @DisplayName("EXIT sem ENTRY prévia -> ActiveSessionNotFoundException")
        void exitSemEntryPrevia_throwsActiveSessionNotFoundException() {
            assertThatThrownBy(() -> exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME))
                    .isInstanceOf(ActiveSessionNotFoundException.class)
                    .hasMessageContaining(LICENSE_PLATE);
        }
    }
}
