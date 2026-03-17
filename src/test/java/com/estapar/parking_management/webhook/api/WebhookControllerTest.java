package com.estapar.parking_management.webhook.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.estapar.parking_management.parking.application.EntryVehicleService;
import com.estapar.parking_management.parking.application.ExitVehicleService;
import com.estapar.parking_management.parking.application.ParkVehicleService;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import com.estapar.parking_management.shared.exception.GarageFullException;
import com.estapar.parking_management.shared.exception.GlobalExceptionHandler;
import com.estapar.parking_management.shared.exception.InvalidSessionTransitionException;
import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;
import com.estapar.parking_management.shared.exception.ValidationException;

@WebMvcTest(WebhookController.class)
@Import(GlobalExceptionHandler.class)
class WebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EntryVehicleService entryVehicleService;
    @MockitoBean
    ParkVehicleService parkVehicleService;
    @MockitoBean
    ExitVehicleService exitVehicleService;

    private static final String ENTRY_PAYLOAD = """
            {"license_plate":"ABC-1234","entry_time":"2024-01-15T10:00:00Z","event_type":"ENTRY"}
            """;
    private static final String PARKED_PAYLOAD = """
            {"license_plate":"ABC-1234","lat":-23.55,"lng":-46.63,"event_type":"PARKED"}
            """;
    private static final String EXIT_PAYLOAD = """
            {"license_plate":"ABC-1234","exit_time":"2024-01-15T12:00:00Z","event_type":"EXIT"}
            """;

    @Nested
    @DisplayName("Cenários de sucesso (200 OK)")
    class SuccessScenarios {

        @Test
        @DisplayName("ENTRY com payload válido retorna message e timestamp")
        void entry_validPayload_returns200WithMessage() throws Exception {
            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(ENTRY_PAYLOAD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Vehicle entry registered successfully"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("PARKED com payload válido retorna message e timestamp")
        void parked_validPayload_returns200WithMessage() throws Exception {
            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(PARKED_PAYLOAD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Vehicle parked successfully"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("EXIT com payload válido retorna message e timestamp")
        void exit_validPayload_returns200WithMessage() throws Exception {
            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(EXIT_PAYLOAD))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Vehicle exit processed successfully"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Cenários de erro - validação (400)")
    class ValidationErrorScenarios {

        @Test
        @DisplayName("Payload sem event_type retorna 400")
        void missingEventType_returns400() throws Exception {
            String payload = """
                    {"license_plate":"ABC-1234","entry_time":"2024-01-15T10:00:00Z"}
                    """;

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("ENTRY sem entry_time retorna 400")
        void entryWithoutEntryTime_returns400() throws Exception {
            String payload = """
                    {"license_plate":"ABC-1234","event_type":"ENTRY"}
                    """;
            doThrow(new ValidationException("entryTime must not be null"))
                    .when(entryVehicleService).processEntry(any(), isNull());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("ENTRY sem license_plate retorna 400")
        void entryWithoutLicensePlate_returns400() throws Exception {
            String payload = """
                    {"entry_time":"2024-01-15T10:00:00Z","event_type":"ENTRY"}
                    """;
            doThrow(new ValidationException("licensePlate must not be null or blank"))
                    .when(entryVehicleService).processEntry(isNull(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("PARKED sem lat retorna 400")
        void parkedWithoutLat_returns400() throws Exception {
            String payload = """
                    {"license_plate":"ABC-1234","lng":-46.63,"event_type":"PARKED"}
                    """;
            doThrow(new ValidationException("lat must not be null"))
                    .when(parkVehicleService).processParked(any(), isNull(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("PARKED sem lng retorna 400")
        void parkedWithoutLng_returns400() throws Exception {
            String payload = """
                    {"license_plate":"ABC-1234","lat":-23.55,"event_type":"PARKED"}
                    """;
            doThrow(new ValidationException("lng must not be null"))
                    .when(parkVehicleService).processParked(any(), any(), isNull());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("EXIT sem exit_time retorna 400")
        void exitWithoutExitTime_returns400() throws Exception {
            String payload = """
                    {"license_plate":"ABC-1234","event_type":"EXIT"}
                    """;
            doThrow(new ValidationException("exitTime must not be null"))
                    .when(exitVehicleService).processExit(any(), isNull());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("Cenários de erro - negócio (409/404)")
    class BusinessErrorScenarios {

        @Test
        @DisplayName("GarageFullException retorna 409")
        void garageFullException_returns409() throws Exception {
            doThrow(new GarageFullException("Garage is full"))
                    .when(entryVehicleService).processEntry(any(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(ENTRY_PAYLOAD))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GARAGE_FULL"))
                    .andExpect(jsonPath("$.message").value("Garage is full"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("ActiveSessionNotFoundException retorna 404")
        void activeSessionNotFoundException_returns404() throws Exception {
            doThrow(new ActiveSessionNotFoundException("XYZ-9876"))
                    .when(exitVehicleService).processExit(any(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(EXIT_PAYLOAD))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ACTIVE_SESSION_NOT_FOUND"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("SpotAlreadyOccupiedException retorna 409")
        void spotAlreadyOccupiedException_returns409() throws Exception {
            doThrow(new SpotAlreadyOccupiedException("Spot is already occupied"))
                    .when(parkVehicleService).processParked(any(), any(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(PARKED_PAYLOAD))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SPOT_ALREADY_OCCUPIED"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("InvalidSessionTransitionException retorna 409")
        void invalidSessionTransitionException_returns409() throws Exception {
            doThrow(new InvalidSessionTransitionException("ENTERED", "PARKED"))
                    .when(parkVehicleService).processParked(any(), any(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(PARKED_PAYLOAD))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_SESSION_TRANSITION"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Cenários de erro - inesperado (500)")
    class UnexpectedErrorScenarios {

        @Test
        @DisplayName("RuntimeException retorna 500")
        void runtimeException_returns500() throws Exception {
            doThrow(new RuntimeException("Unexpected error"))
                    .when(entryVehicleService).processEntry(any(), any());

            mockMvc.perform(post("/webhook")
                            .contentType(APPLICATION_JSON)
                            .content(ENTRY_PAYLOAD))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
