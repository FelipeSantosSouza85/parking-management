package com.estapar.parking_management.garage.application;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.garage.infrastructure.client.GarageSimulatorClient;
import com.estapar.parking_management.garage.infrastructure.client.GarageSimulatorClient.GarageSimulatorException;
import com.estapar.parking_management.garage.infrastructure.client.dto.GarageConfigurationResponse;
import com.estapar.parking_management.garage.infrastructure.client.dto.SectorResponse;
import com.estapar.parking_management.garage.infrastructure.client.dto.SpotResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GarageSynchronizationService")
@ExtendWith(MockitoExtension.class)
class GarageSynchronizationServiceTest {

    @Mock
    private GarageSimulatorClient simulatorClient;

    @Mock
    private GarageSectorPort sectorPort;

    @Mock
    private ParkingSpotPort spotPort;

    @Mock
    private GarageOccupancyPort occupancyPort;

    @Captor
    private ArgumentCaptor<List<GarageSector>> sectorListCaptor;

    @Captor
    private ArgumentCaptor<List<ParkingSpot>> spotListCaptor;

    @Captor
    private ArgumentCaptor<GarageOccupancy> occupancyCaptor;

    private GarageSynchronizationService service;

    @BeforeEach
    void setUp() {
        service = new GarageSynchronizationService(simulatorClient, sectorPort, spotPort, occupancyPort);
    }

    @Nested
    @DisplayName("Sincronização de setores")
    class SectorSynchronization {

        @Test
        @DisplayName("deve criar novo setor quando não existir")
        void shouldCreateNewSectorWhenNotExists() {
            // Given
            SectorResponse sectorResponse = new SectorResponse(
                    "A", new BigDecimal("40.50"), 10, "08:00", "22:00", 120
            );
            GarageConfigurationResponse config = new GarageConfigurationResponse(
                    List.of(sectorResponse), List.of()
            );

            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);
            when(sectorPort.findAllBySectorCodeIn(List.of("A"))).thenReturn(List.of());
            when(sectorPort.saveAll(any())).thenAnswer(invocation -> {
                List<GarageSector> sectors = invocation.getArgument(0);
                sectors.forEach(s -> { if (s.getId() == null) s.setId(1L); });
                return sectors;
            });

            // When
            service.synchronize();

            // Then
            verify(sectorPort).saveAll(sectorListCaptor.capture());
            GarageSector savedSector = sectorListCaptor.getValue().getFirst();

            assertThat(savedSector.getSectorCode()).isEqualTo("A");
            assertThat(savedSector.getBasePrice()).isEqualByComparingTo("40.50");
            assertThat(savedSector.getMaxCapacity()).isEqualTo(10);
            assertThat(savedSector.getOpenHour()).isEqualTo(LocalTime.of(8, 0));
            assertThat(savedSector.getCloseHour()).isEqualTo(LocalTime.of(22, 0));
            assertThat(savedSector.getDurationLimitMinutes()).isEqualTo(120);
        }

        @Test
        @DisplayName("deve atualizar setor existente preservando identidade")
        void shouldUpdateExistingSector() {
            // Given
            GarageSector existingSector = new GarageSector(
                    "A", new BigDecimal("30.00"), 8, LocalTime.of(7, 0), LocalTime.of(20, 0), 60
            );
            existingSector.setId(1L);

            SectorResponse sectorResponse = new SectorResponse(
                    "A", new BigDecimal("40.50"), 10, "08:00", "22:00", 120
            );
            GarageConfigurationResponse config = new GarageConfigurationResponse(
                    List.of(sectorResponse), List.of()
            );

            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);
            when(sectorPort.findAllBySectorCodeIn(List.of("A"))).thenReturn(List.of(existingSector));
            when(sectorPort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.synchronize();

            // Then
            verify(sectorPort).saveAll(sectorListCaptor.capture());
            GarageSector updatedSector = sectorListCaptor.getValue().getFirst();

            assertThat(updatedSector).isSameAs(existingSector);
            assertThat(updatedSector.getId()).isEqualTo(1L);
            assertThat(updatedSector.getBasePrice()).isEqualByComparingTo("40.50");
            assertThat(updatedSector.getMaxCapacity()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Sincronização de vagas")
    class SpotSynchronization {

        @Test
        @DisplayName("deve criar nova vaga quando não existir")
        void shouldCreateNewSpotWhenNotExists() {
            // Given
            GarageSector sector = new GarageSector(
                    "A", new BigDecimal("40.50"), 10, LocalTime.of(8, 0), LocalTime.of(22, 0), 120
            );
            sector.setId(1L);

            SectorResponse sectorResponse = new SectorResponse(
                    "A", new BigDecimal("40.50"), 10, "08:00", "22:00", 120
            );
            SpotResponse spotResponse = new SpotResponse(1, "A", -23.56, -46.65, false);
            GarageConfigurationResponse config = new GarageConfigurationResponse(
                    List.of(sectorResponse), List.of(spotResponse)
            );

            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);
            when(sectorPort.findAllBySectorCodeIn(List.of("A"))).thenReturn(List.of());
            when(sectorPort.saveAll(any())).thenAnswer(invocation -> {
                List<GarageSector> sectors = invocation.getArgument(0);
                sectors.forEach(s -> { if (s.getId() == null) s.setId(1L); });
                return sectors;
            });
            when(spotPort.findAllByExternalSpotIdIn(List.of(1))).thenReturn(List.of());
            when(spotPort.saveAll(any())).thenAnswer(invocation -> {
                List<ParkingSpot> spots = invocation.getArgument(0);
                spots.forEach(s -> { if (s.getId() == null) s.setId(1L); });
                return spots;
            });

            // When
            service.synchronize();

            // Then
            verify(spotPort).saveAll(spotListCaptor.capture());
            ParkingSpot savedSpot = spotListCaptor.getValue().getFirst();

            assertThat(savedSpot.getExternalSpotId()).isEqualTo(1);
            assertThat(savedSpot.getLat()).isEqualTo(-23.56);
            assertThat(savedSpot.getLng()).isEqualTo(-46.65);
            assertThat(savedSpot.isOccupied()).isFalse();
        }

        @Test
        @DisplayName("deve atualizar vaga existente preservando status de ocupação do simulador")
        void shouldUpdateExistingSpotPreservingOccupiedStatus() {
            // Given
            GarageSector sector = new GarageSector(
                    "A", new BigDecimal("40.50"), 10, LocalTime.of(8, 0), LocalTime.of(22, 0), 120
            );
            sector.setId(1L);

            ParkingSpot existingSpot = new ParkingSpot(1, sector, -23.56, -46.65, false);
            existingSpot.setId(1L);

            SectorResponse sectorResponse = new SectorResponse(
                    "A", new BigDecimal("40.50"), 10, "08:00", "22:00", 120
            );
            SpotResponse spotResponse = new SpotResponse(1, "A", -23.56, -46.65, true);
            GarageConfigurationResponse config = new GarageConfigurationResponse(
                    List.of(sectorResponse), List.of(spotResponse)
            );

            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);
            when(sectorPort.findAllBySectorCodeIn(List.of("A"))).thenReturn(List.of(sector));
            when(sectorPort.saveAll(any())).thenReturn(List.of(sector));
            when(spotPort.findAllByExternalSpotIdIn(List.of(1))).thenReturn(List.of(existingSpot));
            when(spotPort.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.synchronize();

            // Then
            verify(spotPort).saveAll(spotListCaptor.capture());
            ParkingSpot updatedSpot = spotListCaptor.getValue().getFirst();

            assertThat(updatedSpot).isSameAs(existingSpot);
            assertThat(updatedSpot.getId()).isEqualTo(1L);
            assertThat(updatedSpot.isOccupied()).isTrue();
        }

        @Test
        @DisplayName("deve ignorar vaga com setor desconhecido")
        void shouldIgnoreSpotWithUnknownSector() {
            // Given
            SectorResponse sectorResponse = new SectorResponse(
                    "A", new BigDecimal("40.50"), 10, "08:00", "22:00", 120
            );
            SpotResponse spotWithUnknownSector = new SpotResponse(1, "UNKNOWN", -23.56, -46.65, false);
            GarageConfigurationResponse config = new GarageConfigurationResponse(
                    List.of(sectorResponse), List.of(spotWithUnknownSector)
            );

            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);
            when(sectorPort.findAllBySectorCodeIn(List.of("A"))).thenReturn(List.of());
            when(sectorPort.saveAll(any())).thenAnswer(invocation -> {
                List<GarageSector> sectors = invocation.getArgument(0);
                sectors.forEach(s -> { if (s.getId() == null) s.setId(1L); });
                return sectors;
            });

            // When
            service.synchronize();

            // Then
            verify(spotPort, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Cálculo de ocupação")
    class OccupancyCalculation {

        @Test
        @DisplayName("deve calcular ocupação a partir dos dados do simulador")
        void shouldCalculateOccupancyFromSimulatorData() {
            // Given
            SectorResponse sectorA = new SectorResponse("A", new BigDecimal("40.50"), 10, "08:00", "22:00", 120);
            SectorResponse sectorB = new SectorResponse("B", new BigDecimal("30.00"), 5, "08:00", "22:00", 60);

            SpotResponse spot1 = new SpotResponse(1, "A", -23.56, -46.65, true);
            SpotResponse spot2 = new SpotResponse(2, "A", -23.57, -46.66, false);
            SpotResponse spot3 = new SpotResponse(3, "B", -23.58, -46.67, true);

            GarageConfigurationResponse config = new GarageConfigurationResponse(
                    List.of(sectorA, sectorB), List.of(spot1, spot2, spot3)
            );

            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);
            when(sectorPort.findAllBySectorCodeIn(any())).thenReturn(List.of());
            when(sectorPort.saveAll(any())).thenAnswer(invocation -> {
                List<GarageSector> sectors = invocation.getArgument(0);
                sectors.forEach(s -> s.setId((long) s.getSectorCode().hashCode()));
                return sectors;
            });
            when(spotPort.findAllByExternalSpotIdIn(any())).thenReturn(List.of());
            when(spotPort.saveAll(any())).thenAnswer(invocation -> {
                List<ParkingSpot> spots = invocation.getArgument(0);
                spots.forEach(s -> s.setId((long) s.getExternalSpotId()));
                return spots;
            });

            // When
            service.synchronize();

            // Then
            verify(occupancyPort).deleteAll();
            verify(occupancyPort).save(occupancyCaptor.capture());
            GarageOccupancy occupancy = occupancyCaptor.getValue();

            assertThat(occupancy.getTotalCapacity()).isEqualTo(15);
            assertThat(occupancy.getOccupiedCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Tratamento de erros")
    class ErrorHandling {

        @Test
        @DisplayName("deve propagar exceção quando client falhar")
        void shouldPropagateExceptionWhenClientFails() {
            // Given
            when(simulatorClient.fetchGarageConfiguration())
                    .thenThrow(new GarageSimulatorException("Connection refused"));

            // When/Then
            assertThatThrownBy(() -> service.synchronize())
                    .isInstanceOf(GarageSimulatorException.class)
                    .hasMessageContaining("Connection refused");
        }

        @Test
        @DisplayName("deve lançar exceção quando lista de setores estiver vazia")
        void shouldThrowWhenSectorsListIsEmpty() {
            // Given
            GarageConfigurationResponse config = new GarageConfigurationResponse(List.of(), List.of());
            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);

            // When/Then
            assertThatThrownBy(() -> service.synchronize())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one sector");

            verify(sectorPort, never()).saveAll(any());
        }

        @Test
        @DisplayName("deve normalizar lista nula de setores para vazia e lançar exceção")
        void shouldNormalizeNullSectorsListAndThrow() {
            // Given - compact constructor normaliza null → List.of()
            GarageConfigurationResponse config = new GarageConfigurationResponse(null, List.of());
            when(simulatorClient.fetchGarageConfiguration()).thenReturn(config);

            // When/Then
            assertThatThrownBy(() -> service.synchronize())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one sector");

            verify(sectorPort, never()).saveAll(any());
        }
    }
}
