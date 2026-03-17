package com.estapar.parking_management.shared.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExceptionHandlerTestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Nested
    @DisplayName("Exceções de conflito (409)")
    class ConflictExceptions {

        @Test
        @DisplayName("GarageFullException retorna 409, GARAGE_FULL e mensagem correta")
        void garageFullException() throws Exception {
            mockMvc.perform(get("/test/exceptions/garage-full"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GARAGE_FULL"))
                    .andExpect(jsonPath("$.message").value("Garage is full"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("SpotAlreadyOccupiedException retorna 409, SPOT_ALREADY_OCCUPIED e mensagem correta")
        void spotAlreadyOccupiedException() throws Exception {
            mockMvc.perform(get("/test/exceptions/spot-already-occupied"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SPOT_ALREADY_OCCUPIED"))
                    .andExpect(jsonPath("$.message").value("Spot is already occupied"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("ActiveSessionAlreadyExistsException retorna 409, ACTIVE_SESSION_ALREADY_EXISTS e mensagem correta")
        void activeSessionAlreadyExistsException() throws Exception {
            mockMvc.perform(get("/test/exceptions/active-session-already-exists/ABC-1234"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ACTIVE_SESSION_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("Active session already exists for license plate: ABC-1234"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("InvalidSessionTransitionException retorna 409, INVALID_SESSION_TRANSITION e mensagem correta")
        void invalidSessionTransitionException() throws Exception {
            mockMvc.perform(get("/test/exceptions/invalid-session-transition/ENTERED/PARKED"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_SESSION_TRANSITION"))
                    .andExpect(jsonPath("$.message").value("Invalid session transition from ENTERED to PARKED"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Exceções de recurso não encontrado (404)")
    class NotFoundExceptions {

        @Test
        @DisplayName("SpotNotFoundException retorna 404, SPOT_NOT_FOUND e mensagem correta")
        void spotNotFoundException() throws Exception {
            mockMvc.perform(get("/test/exceptions/spot-not-found/-23.55/-46.63"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SPOT_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Spot not found at coordinates: lat=-23.55, lng=-46.63"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("ActiveSessionNotFoundException retorna 404, ACTIVE_SESSION_NOT_FOUND e mensagem correta")
        void activeSessionNotFoundException() throws Exception {
            mockMvc.perform(get("/test/exceptions/active-session-not-found/XYZ-9876"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ACTIVE_SESSION_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Active session not found for license plate: XYZ-9876"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
