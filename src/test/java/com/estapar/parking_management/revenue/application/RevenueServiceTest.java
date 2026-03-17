package com.estapar.parking_management.revenue.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.revenue.api.dto.RevenueResponse;
import com.estapar.parking_management.revenue.infrastructure.persistence.RevenueRepository;
import com.estapar.parking_management.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@DisplayName("RevenueService")
@ExtendWith(MockitoExtension.class)
class RevenueServiceTest {

    @Mock
    private GarageSectorPort garageSectorPort;

    @Mock
    private RevenueRepository revenueRepository;

    private RevenueService service;

    @BeforeEach
    void setUp() {
        service = new RevenueService(garageSectorPort, revenueRepository);
    }

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessScenarios {

        @Test
        @DisplayName("setor válido com receita existente retorna valor correto")
        void validSectorWithRevenue_returnsCorrectAmount() {
            GarageSector sector = createSector(1L, "A");
            when(garageSectorPort.findBySectorCode("A")).thenReturn(Optional.of(sector));
            when(revenueRepository.sumChargedAmount(1L, LocalDate.of(2025, 1, 1)))
                    .thenReturn(new BigDecimal("121.50"));

            RevenueResponse response = service.getRevenue(LocalDate.of(2025, 1, 1), "A");

            assertThat(response.amount()).isEqualByComparingTo("121.50");
            assertThat(response.currency()).isEqualTo("BRL");
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("setor válido sem sessões retorna amount = 0")
        void validSectorWithoutSessions_returnsZero() {
            GarageSector sector = createSector(1L, "A");
            when(garageSectorPort.findBySectorCode("A")).thenReturn(Optional.of(sector));
            when(revenueRepository.sumChargedAmount(1L, LocalDate.of(2025, 1, 1)))
                    .thenReturn(BigDecimal.ZERO);

            RevenueResponse response = service.getRevenue(LocalDate.of(2025, 1, 1), "A");

            assertThat(response.amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.currency()).isEqualTo("BRL");
        }
    }

    @Nested
    @DisplayName("Cenários de erro - recurso não encontrado")
    class NotFoundScenarios {

        @Test
        @DisplayName("setor inexistente lança ResourceNotFoundException")
        void unknownSector_throwsResourceNotFoundException() {
            when(garageSectorPort.findBySectorCode("Z")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRevenue(LocalDate.of(2025, 1, 1), "Z"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Sector not found: Z");

            verify(garageSectorPort).findBySectorCode("Z");
            verify(revenueRepository, never()).sumChargedAmount(any(), any());
        }
    }

    private static GarageSector createSector(Long id, String sectorCode) {
        GarageSector sector = new GarageSector(
                sectorCode,
                new BigDecimal("40.50"),
                10,
                LocalTime.of(8, 0),
                LocalTime.of(22, 0),
                120
        );
        sector.setId(id);
        return sector;
    }
}
