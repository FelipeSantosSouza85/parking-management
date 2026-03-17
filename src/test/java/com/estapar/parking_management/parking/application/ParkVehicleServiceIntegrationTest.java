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
import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;
import com.estapar.parking_management.shared.exception.SpotNotFoundException;
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
@DisplayName("ParkVehicleService (integration)")
class ParkVehicleServiceIntegrationTest {

    private static final String LICENSE_PLATE = "ZUL0001";
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.ofInstant(Instant.parse("2025-03-13T10:00:00Z"), ZoneOffset.UTC);
    private static final Double SPOT_LAT = -23.561684;
    private static final Double SPOT_LNG = -46.655981;

    @Autowired
    private ParkVehicleService parkVehicleService;

    @Autowired
    private EntryVehicleService entryVehicleService;

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
        occupancy.setOccupiedCount(30); // 30% occupancy -> adjustment 0 -> hourlyPrice = 10.00
        garageOccupancyPort.save(occupancy);

        sector = garageSectorPort.save(
                new GarageSector("A", new BigDecimal("10.00"), 100,
                        LocalTime.of(8, 0), LocalTime.of(22, 0), 120));

        spot = parkingSpotPort.save(new ParkingSpot(1, sector, SPOT_LAT, SPOT_LNG, false));
    }

    @Nested
    @DisplayName("PARKED successfully")
    class ParkedComSucesso {

        @Test
        @DisplayName("verifies real persistence (status, spot, sector, hourlyPriceApplied, parkedTime)")
        void verificaPersistenciaRealNoBanco() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            Optional<ParkingSession> resultOpt = parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE);
            assertThat(resultOpt).isPresent();
            ParkingSession result = resultOpt.get();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ParkingSessionStatus.PARKED);
            assertThat(result.getParkingSpot()).isNotNull();
            assertThat(result.getParkingSpot().getId()).isEqualTo(spot.getId());
            assertThat(result.getSector()).isNotNull();
            assertThat(result.getSector().getId()).isEqualTo(sector.getId());
            assertThat(result.getHourlyPriceApplied()).isNotNull();
            assertThat(result.getHourlyPriceApplied()).isEqualByComparingTo("10.00");
            assertThat(result.getParkedTime()).isNotNull();
            assertThat(result.getParkedTime()).isAfter(ENTRY_TIME.toInstant(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("verifies that the spot became occupied in the database")
        void verificaSpotFicouOcupadoNoBanco() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            Optional<ParkingSpot> foundSpot = parkingSpotPort.findById(spot.getId());
            assertThat(foundSpot).isPresent();
            assertThat(foundSpot.get().isOccupied()).isTrue();
        }
    }

    @Nested
    @DisplayName("Sessão não encontrada")
    class SessaoNaoEncontrada {

        @Test
        @DisplayName("PARKED sem ENTRY prévia -> ActiveSessionNotFoundException")
        void parkedSemEntryPrevia_throwsActiveSessionNotFoundException() {
            assertThatThrownBy(() -> parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG))
                    .isInstanceOf(ActiveSessionNotFoundException.class)
                    .hasMessageContaining(LICENSE_PLATE);
        }
    }

    @Nested
    @DisplayName("Spot não encontrado")
    class SpotNaoEncontrado {

        @Test
        @DisplayName("PARKED com coordenadas inválidas -> SpotNotFoundException")
        void parkedComCoordenadasInvalidas_throwsSpotNotFoundException() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            Double invalidLat = 0.0;
            Double invalidLng = 0.0;

            assertThatThrownBy(() -> parkVehicleService.processParked(LICENSE_PLATE, invalidLat, invalidLng))
                    .isInstanceOf(SpotNotFoundException.class)
                    .hasMessageContaining("0.0");
        }
    }

    @Nested
    @DisplayName("Spot já ocupado")
    class SpotJaOcupado {

        @Test
        @DisplayName("ocupar spot manualmente, tentar PARKED -> SpotAlreadyOccupiedException")
        void spotJaOcupado_throwsSpotAlreadyOccupiedException() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            spot.occupy();
            parkingSpotPort.save(spot);

            assertThatThrownBy(() -> parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG))
                    .isInstanceOf(SpotAlreadyOccupiedException.class)
                    .hasMessageContaining("already occupied");
        }
    }
}
