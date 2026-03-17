package com.estapar.parking_management.revenue.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.estapar.parking_management.revenue.api.dto.RevenueResponse;
import com.estapar.parking_management.revenue.application.RevenueService;
import com.estapar.parking_management.shared.exception.GlobalExceptionHandler;
import com.estapar.parking_management.shared.exception.ResourceNotFoundException;

@WebMvcTest(RevenueController.class)
@Import(GlobalExceptionHandler.class)
class RevenueControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RevenueService revenueService;

    @Nested
    @DisplayName("Cenários de sucesso (200 OK)")
    class SuccessScenarios {

        @Test
        @DisplayName("consulta válida retorna amount, currency e timestamp")
        void validQuery_returns200WithAmountCurrencyAndTimestamp() throws Exception {
            RevenueResponse response = new RevenueResponse(
                    new BigDecimal("121.50"),
                    "BRL",
                    Instant.parse("2025-01-01T12:00:00Z")
            );

            when(revenueService.getRevenue(eq(LocalDate.of(2025, 1, 1)), eq("A")))
                    .thenReturn(response);

            mockMvc.perform(get("/revenue")
                            .param("date", "2025-01-01")
                            .param("sector", "A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.amount").value(121.50))
                    .andExpect(jsonPath("$.currency").value("BRL"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("Cenários de erro - parâmetros ausentes (400)")
    class MissingParamsScenarios {

        @Test
        @DisplayName("parâmetro date ausente retorna 400")
        void missingDate_returns400() throws Exception {
            mockMvc.perform(get("/revenue")
                            .param("sector", "A"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("parâmetro sector ausente retorna 400")
        void missingSector_returns400() throws Exception {
            mockMvc.perform(get("/revenue")
                            .param("date", "2025-01-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("formato de data inválido retorna 400")
        void invalidDateFormat_returns400() throws Exception {
            mockMvc.perform(get("/revenue")
                            .param("date", "2025/01/01")
                            .param("sector", "A"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("setor em branco retorna 400")
        void blankSector_returns400() throws Exception {
            mockMvc.perform(get("/revenue")
                            .param("date", "2025-01-01")
                            .param("sector", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("Cenários de erro - negócio (404)")
    class BusinessErrorScenarios {

        @Test
        @DisplayName("setor inexistente retorna 404")
        void sectorNotFound_returns404() throws Exception {
            when(revenueService.getRevenue(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Sector not found: Z"));

            mockMvc.perform(get("/revenue")
                            .param("date", "2025-01-01")
                            .param("sector", "Z"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Sector not found: Z"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
}
