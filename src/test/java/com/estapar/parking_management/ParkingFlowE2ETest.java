package com.estapar.parking_management;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E tests via HTTP for the full parking flow.
 * Uses MockMvc with full context, without @Transactional to ensure real commits.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("ParkingFlow E2E")
class ParkingFlowE2ETest {

    private static final String LICENSE_PLATE = "ZUL0001";
    private static final Double SPOT_LAT = -23.561684;
    private static final Double SPOT_LNG = -46.655981;

    @Autowired
    MockMvc mockMvc;

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
    @DisplayName("Full flow ENTRY -> PARKED -> EXIT")
    class FluxoCompleto {

        @Test
        @DisplayName("sends 3 POSTs to /webhook and verifies calculated revenue (2h * 10.00 = 20.00)")
        void fluxoCompletoEntryParkedExit_verificaRevenue() throws Exception {
            String entryPayload = """
                    {"license_plate":"%s","entry_time":"2025-01-01T12:00:00","event_type":"ENTRY"}
                    """.formatted(LICENSE_PLATE);
            String parkedPayload = """
                    {"license_plate":"%s","lat":%s,"lng":%s,"event_type":"PARKED"}
                    """.formatted(LICENSE_PLATE, SPOT_LAT, SPOT_LNG);
            String exitPayload = """
                    {"license_plate":"%s","exit_time":"2025-01-01T14:00:00","event_type":"EXIT"}
                    """.formatted(LICENSE_PLATE);

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(entryPayload))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(parkedPayload))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(exitPayload))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/revenue")
                            .param("date", "2025-01-01")
                            .param("sector", "A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(20.00))
                    .andExpect(jsonPath("$.currency").value("BRL"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Special case ENTRY -> EXIT without PARKED")
    class EntryExitSemParked {

        @Test
        @DisplayName("sends ENTRY + EXIT without PARKED, verifies amount = 0")
        void entryExitSemParked_amountZero() throws Exception {
            String entryPayload = """
                    {"license_plate":"%s","entry_time":"2025-01-01T12:00:00","event_type":"ENTRY"}
                    """.formatted(LICENSE_PLATE);
            String exitPayload = """
                    {"license_plate":"%s","exit_time":"2025-01-01T14:00:00","event_type":"EXIT"}
                    """.formatted(LICENSE_PLATE);

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(entryPayload))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(exitPayload))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/revenue")
                            .param("date", "2025-01-01")
                            .param("sector", "A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(0.00))
                    .andExpect(jsonPath("$.currency").value("BRL"));
        }
    }
}
