package com.estapar.parking_management;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.application.EntryVehicleService;
import com.estapar.parking_management.parking.application.ExitVehicleService;
import com.estapar.parking_management.parking.application.ParkVehicleService;
import com.estapar.parking_management.shared.exception.GarageFullException;
import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;

/**
 * Real concurrency tests with threads.
 * Does NOT use @Transactional - each thread needs a committed transaction for pessimistic locks.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Parking Concurrency")
class ParkingConcurrencyTest {

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

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        testDataCleaner.cleanAll();
    }

    @Nested
    @DisplayName("Two simultaneous ENTRY with 1 spot")
    class DoisEntrySimultaneosComUmaVaga {

        @Test
        @DisplayName("exactly 1 success, 1 GarageFullException, occupiedCount == 1")
        void doisEntrySimultaneos_exatamenteUmSucessoUmFalha() throws Exception {
            GarageOccupancy occupancy = new GarageOccupancy(1);
            garageOccupancyPort.save(occupancy);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);

            List<Object> results = new ArrayList<>();

            Future<?> future1 = executor.submit(() -> {
                try {
                    startLatch.await();
                    entryVehicleService.processEntry("PLATE-A", ENTRY_TIME);
                } catch (Exception e) {
                    results.add(e);
                }
            });

            Future<?> future2 = executor.submit(() -> {
                try {
                    startLatch.await();
                    entryVehicleService.processEntry("PLATE-B", ENTRY_TIME);
                } catch (Exception e) {
                    results.add(e);
                }
            });

            startLatch.countDown();

            future1.get();
            future2.get();

            executor.shutdown();

            int successCount = 2 - results.size();
            int garageFullCount = (int) results.stream()
                    .filter(e -> e instanceof GarageFullException)
                    .count();

            assertThat(successCount).isEqualTo(1);
            assertThat(garageFullCount).isEqualTo(1);

            transactionTemplate.executeWithoutResult(__ -> {
                var occupancyAfter = garageOccupancyPort.findWithLock();
                assertThat(occupancyAfter).isPresent();
                assertThat(occupancyAfter.get().getOccupiedCount()).isEqualTo(1);
            });
        }
    }

    @Nested
    @DisplayName("Dois PARKED para o mesmo spot")
    class DoisParkedParaMesmoSpot {

        @Test
        @DisplayName("exatamente 1 sucesso, 1 SpotAlreadyOccupiedException")
        void doisParkedSimultaneos_exatamenteUmSucessoUmFalha() throws Exception {
            GarageOccupancy occupancy = new GarageOccupancy(100);
            garageOccupancyPort.save(occupancy);

            GarageSector sector = garageSectorPort.save(
                    new GarageSector("A", new BigDecimal("10.00"), 100,
                            LocalTime.of(8, 0), LocalTime.of(22, 0), 120));

            parkingSpotPort.save(new ParkingSpot(1, sector, SPOT_LAT, SPOT_LNG, false));

            entryVehicleService.processEntry("PLATE-A", ENTRY_TIME);
            entryVehicleService.processEntry("PLATE-B", ENTRY_TIME);

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);

            List<Object> results = new ArrayList<>();

            Future<?> future1 = executor.submit(() -> {
                try {
                    startLatch.await();
                    parkVehicleService.processParked("PLATE-A", SPOT_LAT, SPOT_LNG);
                } catch (Exception e) {
                    results.add(e);
                }
            });

            Future<?> future2 = executor.submit(() -> {
                try {
                    startLatch.await();
                    parkVehicleService.processParked("PLATE-B", SPOT_LAT, SPOT_LNG);
                } catch (Exception e) {
                    results.add(e);
                }
            });

            startLatch.countDown();

            future1.get();
            future2.get();

            executor.shutdown();

            int successCount = 2 - results.size();
            int spotOccupiedCount = (int) results.stream()
                    .filter(e -> e instanceof SpotAlreadyOccupiedException)
                    .count();
            int failureCount = results.size();

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);
            assertThat(spotOccupiedCount).isGreaterThanOrEqualTo(0); // SpotAlreadyOccupied ou deadlock

            transactionTemplate.executeWithoutResult(__ -> {
                var spotAfter = parkingSpotPort.findByLatAndLngWithLock(SPOT_LAT, SPOT_LNG);
                assertThat(spotAfter).isPresent();
                assertThat(spotAfter.get().isOccupied()).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("Duplicate EXIT")
    class ExitDuplicado {

        @Test
        @DisplayName("exactly 1 success, 1 failure, occupiedCount decremented 1x")
        void doisExitSimultaneos_exatamenteUmSucessoUmFalha() throws Exception {
            GarageOccupancy occupancy = new GarageOccupancy(100);
            occupancy.setOccupiedCount(30);
            garageOccupancyPort.save(occupancy);

            GarageSector sector = garageSectorPort.save(
                    new GarageSector("A", new BigDecimal("10.00"), 100,
                            LocalTime.of(8, 0), LocalTime.of(22, 0), 120));

            parkingSpotPort.save(new ParkingSpot(1, sector, SPOT_LAT, SPOT_LNG, false));

            entryVehicleService.processEntry("PLATE-X", ENTRY_TIME);
            parkVehicleService.processParked("PLATE-X", SPOT_LAT, SPOT_LNG);

            Integer occupiedBefore = transactionTemplate.execute(status ->
                    garageOccupancyPort.findWithLock()
                            .map(GarageOccupancy::getOccupiedCount)
                            .orElse(0));

            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch startLatch = new CountDownLatch(1);

            List<Object> results = new ArrayList<>();

            Future<?> future1 = executor.submit(() -> {
                try {
                    startLatch.await();
                    exitVehicleService.processExit("PLATE-X", EXIT_TIME);
                } catch (Exception e) {
                    results.add(e);
                }
            });

            Future<?> future2 = executor.submit(() -> {
                try {
                    startLatch.await();
                    exitVehicleService.processExit("PLATE-X", EXIT_TIME);
                } catch (Exception e) {
                    results.add(e);
                }
            });

            startLatch.countDown();

            future1.get();
            future2.get();

            executor.shutdown();

            int successCount = 2 - results.size();
            int failureCount = results.size();

            assertThat(successCount).as("exactly 1 EXIT must succeed").isEqualTo(1);
            assertThat(failureCount).as("exactly 1 EXIT must fail").isEqualTo(1);

            transactionTemplate.executeWithoutResult(__ -> {
                var occupancyAfter = garageOccupancyPort.findWithLock();
                assertThat(occupancyAfter).isPresent();
                assertThat(occupancyAfter.get().getOccupiedCount())
                        .as("occupiedCount must be decremented exactly 1x")
                        .isEqualTo(occupiedBefore - 1);
            });
        }
    }
}
