package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import com.estapar.parking_management.shared.exception.ValidationException;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("ExitVehicleService")
@ExtendWith(MockitoExtension.class)
class ExitVehicleServiceTest {

    private static final String LICENSE_PLATE = "ZUL0001";
    private static final Instant ENTRY_TIME = Instant.parse("2025-03-13T10:00:00Z");
    private static final LocalDateTime EXIT_TIME = LocalDateTime.of(2025, 3, 13, 12, 0, 0);
    private static final Instant EXIT_INSTANT = EXIT_TIME.toInstant(ZoneOffset.UTC);
    private static final Double LAT = -23.561684;
    private static final Double LNG = -46.655981;

    @Mock
    private ParkingSessionPort parkingSessionPort;

    @Mock
    private ParkingSpotPort parkingSpotPort;

    @Mock
    private GarageOccupancyPort garageOccupancyPort;

    @Mock
    private PricingCalculator pricingCalculator;

    @Captor
    private ArgumentCaptor<ParkingSession> sessionCaptor;

    @Captor
    private ArgumentCaptor<ParkingSpot> spotCaptor;

    @Captor
    private ArgumentCaptor<GarageOccupancy> occupancyCaptor;

    private ExitVehicleService service;

    @BeforeEach
    void setUp() {
        service = new ExitVehicleService(
                parkingSessionPort,
                parkingSpotPort,
                garageOccupancyPort,
                pricingCalculator
        );
    }

    @Nested
    @DisplayName("Validação de entrada")
    class ValidacaoEntrada {

        @Test
        @DisplayName("licensePlate null -> ValidationException")
        void licensePlateNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processExit(null, EXIT_TIME))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("licensePlate blank -> ValidationException")
        void licensePlateBlank_throwsValidationException() {
            assertThatThrownBy(() -> service.processExit("   ", EXIT_TIME))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("exitTime null -> ValidationException")
        void exitTimeNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processExit(LICENSE_PLATE, (LocalDateTime) null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("exitTime");
        }
    }

    @Nested
    @DisplayName("Regras de negócio")
    class RegrasNegocio {

        @Test
        @DisplayName("Sessão ativa não encontrada -> ActiveSessionNotFoundException")
        void sessaoAtivaNaoEncontrada_throwsActiveSessionNotFoundException() {
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processExit(LICENSE_PLATE, EXIT_TIME))
                    .isInstanceOf(ActiveSessionNotFoundException.class)
                    .hasMessageContaining(LICENSE_PLATE);

            verify(parkingSpotPort, never()).save(any());
            verify(garageOccupancyPort, never()).findWithLock();
            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("GarageOccupancy não encontrada -> IllegalStateException")
        void garageOccupancyNaoEncontrada_throwsIllegalStateException() {
            ParkingSession session = createParkedSession();
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processExit(LICENSE_PLATE, EXIT_TIME))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("GarageOccupancy not found");

            verify(parkingSessionPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Fluxo de sucesso - Sessão PARKED")
    class FluxoSucessoParked {

        @Test
        @DisplayName("Status atualizado para EXITED")
        void statusAtualizadoParaExited() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.EXITED);
        }

        @Test
        @DisplayName("chargedAmount calculado corretamente (ex: 2h * R$10.00 = R$20.00)")
        void chargedAmountCalculadoCorretamente() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();
            BigDecimal hourlyPrice = new BigDecimal("10.00");
            session.setHourlyPriceApplied(hourlyPrice);

            Duration duration = Duration.between(ENTRY_TIME, EXIT_INSTANT);
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(duration)).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(hourlyPrice, 2)).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getChargedAmount()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("exitTime definido corretamente")
        void exitTimeDefinidoCorretamente() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getExitTime()).isEqualTo(EXIT_INSTANT);
        }

        @Test
        @DisplayName("Spot liberado (release() chamado, occupied = false)")
        void spotLiberado() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(spotCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSpot savedSpot = spotCaptor.getValue();
            assertThat(savedSpot.isOccupied()).isFalse();
        }

        @Test
        @DisplayName("occupiedCount decrementado em GarageOccupancy")
        void occupiedCountDecrementado() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();
            int initialCount = occupancy.getOccupiedCount();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(occupancyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            GarageOccupancy savedOccupancy = occupancyCaptor.getValue();
            assertThat(savedOccupancy.getOccupiedCount()).isEqualTo(initialCount - 1);
        }

        @Test
        @DisplayName("Sessão salva via port")
        void sessaoSalvaViaPort() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured).isNotNull();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.EXITED);
            verify(parkingSessionPort).save(any(ParkingSession.class));
        }
    }

    @Nested
    @DisplayName("Fluxo de sucesso - Sessão ENTERED sem spot")
    class FluxoSucessoEntered {

        @Test
        @DisplayName("Status atualizado para EXITED")
        void statusAtualizadoParaExited() {
            ParkingSession session = createEnteredSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.EXITED);
        }

        @Test
        @DisplayName("chargedAmount = BigDecimal.ZERO")
        void chargedAmountZero() {
            ParkingSession session = createEnteredSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getChargedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Spot não é tocado (parkingSpotPort.save nunca chamado)")
        void spotNaoTocado() {
            ParkingSession session = createEnteredSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            verify(parkingSpotPort, never()).save(any());
        }

        @Test
        @DisplayName("occupiedCount decrementado")
        void occupiedCountDecrementado() {
            ParkingSession session = createEnteredSession();
            GarageOccupancy occupancy = createOccupancy();
            int initialCount = occupancy.getOccupiedCount();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(occupancyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            GarageOccupancy savedOccupancy = occupancyCaptor.getValue();
            assertThat(savedOccupancy.getOccupiedCount()).isEqualTo(initialCount - 1);
        }
    }

    @Nested
    @DisplayName("Caso especial - Tolerância gratuita")
    class ToleranciaGratuita {

        @Test
        @DisplayName("Duração <= 30 min -> chargedAmount = 0")
        void duracaoAte30Min_chargedAmountZero() {
            LocalDateTime exitShort = LocalDateTime.ofInstant(ENTRY_TIME.plus(Duration.ofMinutes(20)), ZoneOffset.UTC);
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(Duration.ofMinutes(20))).thenReturn(0);
            when(pricingCalculator.calculateChargedAmount(any(), eq(0))).thenReturn(BigDecimal.ZERO);
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, exitShort);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getChargedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Duração = 31 min -> cobra 1 hora")
        void duracao31Min_cobra1Hora() {
            LocalDateTime exit31Min = LocalDateTime.ofInstant(ENTRY_TIME.plus(Duration.ofMinutes(31)), ZoneOffset.UTC);
            ParkingSession session = createParkedSession();
            BigDecimal hourlyPrice = new BigDecimal("10.00");
            session.setHourlyPriceApplied(hourlyPrice);
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(Duration.ofMinutes(31))).thenReturn(1);
            when(pricingCalculator.calculateChargedAmount(hourlyPrice, 1)).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, exit31Min);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getChargedAmount()).isEqualByComparingTo("10.00");
        }
    }

    @Nested
    @DisplayName("Interações")
    class Interacoes {

        @Test
        @DisplayName("findActiveByLicensePlateWithLock chamado")
        void verificaFindActiveByLicensePlateWithLockChamado() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            verify(parkingSessionPort).findActiveByLicensePlateWithLock(LICENSE_PLATE);
        }

        @Test
        @DisplayName("findWithLock (occupancy) chamado")
        void verificaFindWithLockOccupancyChamado() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            verify(garageOccupancyPort).findWithLock();
        }

        @Test
        @DisplayName("Saves chamados no sucesso (PARKED)")
        void verificaSavesChamadosNoSucessoParked() {
            ParkingSession session = createParkedSession();
            GarageOccupancy occupancy = createOccupancy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(pricingCalculator.calculateChargeableHours(any())).thenReturn(2);
            when(pricingCalculator.calculateChargedAmount(any(), anyInt())).thenReturn(new BigDecimal("20.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(garageOccupancyPort.findWithLock()).thenReturn(Optional.of(occupancy));
            when(garageOccupancyPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processExit(LICENSE_PLATE, EXIT_TIME);

            verify(parkingSpotPort).save(any(ParkingSpot.class));
            verify(garageOccupancyPort).save(any(GarageOccupancy.class));
            verify(parkingSessionPort).save(any(ParkingSession.class));
        }

        @Test
        @DisplayName("Nenhum save em cenários de erro")
        void nenhumSaveEmCenariosDeErro() {
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processExit(LICENSE_PLATE, EXIT_TIME))
                    .isInstanceOf(ActiveSessionNotFoundException.class);

            verify(parkingSpotPort, never()).save(any());
            verify(garageOccupancyPort, never()).save(any());
            verify(parkingSessionPort, never()).save(any());
        }
    }

    private ParkingSession createParkedSession() {
        ParkingSession session = new ParkingSession(LICENSE_PLATE, ENTRY_TIME, ParkingSessionStatus.PARKED);
        session.setId(1L);
        session.setOccupancyRateAtEntry(BigDecimal.ZERO);
        session.setPriceAdjustmentRateAtEntry(BigDecimal.ZERO);
        session.setHourlyPriceApplied(new BigDecimal("10.00"));
        GarageSector sector = createSector();
        ParkingSpot spot = new ParkingSpot(1, sector, LAT, LNG, false);
        spot.occupy();
        session.setParkingSpot(spot);
        session.setSector(sector);
        return session;
    }

    private ParkingSession createEnteredSession() {
        ParkingSession session = new ParkingSession(LICENSE_PLATE, ENTRY_TIME, ParkingSessionStatus.ENTERED);
        session.setId(1L);
        session.setOccupancyRateAtEntry(BigDecimal.ZERO);
        session.setPriceAdjustmentRateAtEntry(BigDecimal.ZERO);
        return session;
    }

    private GarageSector createSector() {
        return new GarageSector(
                "A",
                new BigDecimal("10.00"),
                100,
                LocalTime.of(8, 0),
                LocalTime.of(22, 0),
                120
        );
    }

    private GarageOccupancy createOccupancy() {
        GarageOccupancy oc = new GarageOccupancy(100);
        oc.setId(1L);
        oc.setOccupiedCount(31);
        return oc;
    }
}
