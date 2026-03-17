package com.estapar.parking_management;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.application.EntryVehicleService;
import com.estapar.parking_management.parking.application.ExitVehicleService;
import com.estapar.parking_management.parking.application.ParkVehicleService;
import com.estapar.parking_management.shared.exception.ActiveSessionAlreadyExistsException;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import com.estapar.parking_management.shared.exception.InvalidSessionTransitionException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Idempotency tests by session state.
 * Correct rejection of repeated/invalid operations according to current state.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("Session State Idempotency")
class SessionStateIdempotencyTest {

    private static final String LICENSE_PLATE = "ZUL0001";
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.ofInstant(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime EXIT_TIME = LocalDateTime.ofInstant(Instant.parse("2025-01-01T14:00:00Z"), ZoneOffset.UTC);
    private static final Double SPOT_LAT = -23.561684;
    private static final Double SPOT_LNG = -46.655981;

    @Autowired
    EntryVehicleService entryVehicleService;

    @Autowired
    ParkVehicleService parkVehicleService;

    @Autowired
    ExitVehicleService exitVehicleService;

    @Autowired
    TestDataCleaner testDataCleaner;

    @Autowired
    GarageOccupancyPort garageOccupancyPort;

    @Autowired
    GarageSectorPort garageSectorPort;

    @Autowired
    ParkingSpotPort parkingSpotPort;

    @BeforeEach
    void setUp() {
        testDataCleaner.cleanAll();

        GarageOccupancy occupancy = new GarageOccupancy(100);
        occupancy.setOccupiedCount(30);
        garageOccupancyPort.save(occupancy);

        GarageSector sector = garageSectorPort.save(
                new GarageSector("A", new BigDecimal("10.00"), 100,
                        LocalTime.of(8, 0), LocalTime.of(22, 0), 120));

        parkingSpotPort.save(new ParkingSpot(1, sector, SPOT_LAT, SPOT_LNG, false));
    }

    @Nested
    @DisplayName("Duplicate ENTRY for same plate")
    class EntryDuplicada {

        @Test
        @DisplayName("processEntry 2x -> ActiveSessionAlreadyExistsException")
        void entryDuplicada_throwsActiveSessionAlreadyExistsException() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);

            assertThatThrownBy(() -> entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(ActiveSessionAlreadyExistsException.class)
                    .hasMessageContaining(LICENSE_PLATE);
        }
    }

    @Nested
    @DisplayName("PARKED on session already PARKED")
    class ParkedEmSessaoJaParked {

        @Test
        @DisplayName("ENTRY -> PARKED -> PARKED novamente -> InvalidSessionTransitionException")
        void parkedDuplicado_throwsInvalidSessionTransitionException() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);

            assertThatThrownBy(() -> parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG))
                    .isInstanceOf(InvalidSessionTransitionException.class);
        }
    }

    @Nested
    @DisplayName("EXIT em sessão já EXITED")
    class ExitEmSessaoJaExited {

        @Test
        @DisplayName("ENTRY -> PARKED -> EXIT -> EXIT novamente -> ActiveSessionNotFoundException")
        void exitDuplicado_throwsActiveSessionNotFoundException() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);
            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            assertThatThrownBy(() -> exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME))
                    .isInstanceOf(ActiveSessionNotFoundException.class)
                    .hasMessageContaining(LICENSE_PLATE);
        }
    }

    @Nested
    @DisplayName("PARKED on session already EXITED")
    class ParkedEmSessaoJaExited {

        @Test
        @DisplayName("ENTRY -> PARKED -> EXIT -> PARKED novamente -> ActiveSessionNotFoundException")
        void parkedAposExit_throwsActiveSessionNotFoundException() {
            entryVehicleService.processEntry(LICENSE_PLATE, ENTRY_TIME);
            parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);
            exitVehicleService.processExit(LICENSE_PLATE, EXIT_TIME);

            assertThatThrownBy(() -> parkVehicleService.processParked(LICENSE_PLATE, SPOT_LAT, SPOT_LNG))
                    .isInstanceOf(ActiveSessionNotFoundException.class)
                    .hasMessageContaining(LICENSE_PLATE);
        }
    }
}
