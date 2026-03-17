package com.estapar.parking_management.parking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionAlreadyExistsException;
import com.estapar.parking_management.shared.exception.GarageFullException;
import com.estapar.parking_management.shared.exception.ValidationException;

@DisplayName("EntryVehicleService")
@ExtendWith(MockitoExtension.class)
class EntryVehicleServiceTest {

    private static final String LICENSE_PLATE = "ABC-1234";
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(2025, 3, 13, 10, 0, 0);

    @Mock
    private ParkingSessionPort parkingSessionPort;

    @Mock
    private GarageOccupancyPort garageOccupancyPort;

    @Mock
    private PricingAdjustmentPolicy pricingAdjustmentPolicy;

    @Captor
    private ArgumentCaptor<ParkingSession> sessionCaptor;

    @Captor
    private ArgumentCaptor<GarageOccupancy> occupancyCaptor;

    private EntryVehicleService service;

    @BeforeEach
    void setUp() {
        service = new EntryVehicleService(parkingSessionPort, garageOccupancyPort, pricingAdjustmentPolicy);
    }

    @Nested
    @DisplayName("Validação de entrada")
    class ValidacaoEntrada {

        @Test
        @DisplayName("licensePlate null -> ValidationException")
        void licensePlateNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processEntry(null, ENTRY_TIME))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("licensePlate blank -> ValidationException")
        void licensePlateBlank_throwsValidationException() {
            assertThatThrownBy(() -> service.processEntry("   ", ENTRY_TIME))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("entryTime null -> ValidationException")
        void entryTimeNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processEntry(LICENSE_PLATE, (LocalDateTime) null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("entryTime");
        }
    }

    @Nested
    @DisplayName("Regras de negócio")
    class RegrasNegocio {

        @Test
        @DisplayName("Sessão ativa já existe -> ActiveSessionAlreadyExistsException")
        void sessaoAtivaJaExiste_throwsActiveSessionAlreadyExistsException() {
            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(true);

            assertThatThrownBy(() -> service.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(ActiveSessionAlreadyExistsException.class)
                    .hasMessageContaining(LICENSE_PLATE);

            verify(garageOccupancyPort, never()).findWithLock();
            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("Garagem cheia (incrementOccupied lança GarageFullException) -> propaga exceção")
        void garagemCheia_propagaGarageFullException() {
            GarageOccupancy occupancy = new GarageOccupancy(1);
            occupancy.incrementOccupied();

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));

            assertThatThrownBy(() -> service.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(GarageFullException.class);

            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("GarageOccupancy não encontrada -> IllegalStateException")
        void garageOccupancyNaoEncontrada_throwsIllegalStateException() {
            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("GarageOccupancy not found");

            verify(parkingSessionPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Fluxo de sucesso")
    class FluxoSucesso {

        @Test
        @DisplayName("Cria sessão com status ENTERED")
        void criaSessaoComStatusEntered() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.ENTERED);
        }

        @Test
        @DisplayName("Captura occupancyRateAtEntry corretamente")
        void capturaOccupancyRateAtEntryCorretamente() {
            GarageOccupancy occupancy = new GarageOccupancy(100);
            occupancy.incrementOccupied();
            occupancy.incrementOccupied();

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getOccupancyRateAtEntry()).isEqualByComparingTo("0.02");
        }

        @Test
        @DisplayName("Captura priceAdjustmentRateAtEntry corretamente")
        void capturaPriceAdjustmentRateAtEntryCorretamente() {
            GarageOccupancy occupancy = new GarageOccupancy(100);
            BigDecimal expectedRate = new BigDecimal("-0.10");

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(expectedRate);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getPriceAdjustmentRateAtEntry()).isEqualByComparingTo(expectedRate);
        }

        @Test
        @DisplayName("Incrementa occupiedCount na GarageOccupancy")
        void incrementaOccupiedCountNaGarageOccupancy() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(occupancyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any(ParkingSession.class))).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            GarageOccupancy savedOccupancy = occupancyCaptor.getValue();
            assertThat(savedOccupancy.getOccupiedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Salva GarageOccupancy atualizada")
        void salvaGarageOccupancyAtualizada() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any(ParkingSession.class))).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            verify(garageOccupancyPort).save(any(GarageOccupancy.class));
        }

        @Test
        @DisplayName("Salva ParkingSession via port")
        void salvaParkingSessionViaPort() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured).isNotNull();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.ENTERED);
            verify(parkingSessionPort).save(any(ParkingSession.class));
        }
    }

    @Nested
    @DisplayName("Interações")
    class Interacoes {

        @Test
        @DisplayName("Verifica que existsActiveByLicensePlate é chamado")
        void verificaExistsActiveByLicensePlateChamado() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any(ParkingSession.class))).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            verify(parkingSessionPort).existsActiveByLicensePlate(LICENSE_PLATE);
        }

        @Test
        @DisplayName("Verifica que findWithLock é chamado")
        void verificaFindWithLockChamado() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any(ParkingSession.class))).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            verify(garageOccupancyPort).findWithLock();
        }

        @Test
        @DisplayName("Verifica que save é chamado para occupancy e session")
        void verificaSaveChamadoParaOccupancyESession() {
            GarageOccupancy occupancy = new GarageOccupancy(100);

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(pricingAdjustmentPolicy.getAdjustmentRate(any(BigDecimal.class))).thenReturn(BigDecimal.ZERO);
            when(garageOccupancyPort.save(any(GarageOccupancy.class))).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any(ParkingSession.class))).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processEntry(LICENSE_PLATE, ENTRY_TIME);

            verify(garageOccupancyPort).save(any(GarageOccupancy.class));
            verify(parkingSessionPort).save(any(ParkingSession.class));
        }

        @Test
        @DisplayName("Não salva session se garagem cheia")
        void naoSalvaSessionSeGaragemCheia() {
            GarageOccupancy occupancy = new GarageOccupancy(1);
            occupancy.incrementOccupied();

            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(false);
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));

            assertThatThrownBy(() -> service.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(GarageFullException.class);

            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("Não incrementa occupancy se sessão já existe")
        void naoIncrementaOccupancySeSessaoJaExiste() {
            when(parkingSessionPort.existsActiveByLicensePlate(LICENSE_PLATE)).thenReturn(true);

            assertThatThrownBy(() -> service.processEntry(LICENSE_PLATE, ENTRY_TIME))
                    .isInstanceOf(ActiveSessionAlreadyExistsException.class);

            verify(garageOccupancyPort, never()).findWithLock();
            verify(garageOccupancyPort, never()).save(any());
        }
    }
}
