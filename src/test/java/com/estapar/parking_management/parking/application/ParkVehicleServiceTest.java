package com.estapar.parking_management.parking.application;

import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.parking.application.port.ParkingSessionPort;
import com.estapar.parking_management.parking.domain.ParkingSession;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import com.estapar.parking_management.shared.exception.ActiveSessionNotFoundException;
import com.estapar.parking_management.shared.exception.InvalidSessionTransitionException;
import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;
import com.estapar.parking_management.shared.exception.SpotNotFoundException;
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
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ParkVehicleService")
@ExtendWith(MockitoExtension.class)
class ParkVehicleServiceTest {

    private static final String LICENSE_PLATE = "ZUL0001";
    private static final Double LAT = -23.561684;
    private static final Double LNG = -46.655981;

    @Mock
    private ParkingSessionPort parkingSessionPort;

    @Mock
    private ParkingSpotPort parkingSpotPort;

    @Mock
    private PricingCalculator pricingCalculator;

    @Captor
    private ArgumentCaptor<ParkingSession> sessionCaptor;

    @Captor
    private ArgumentCaptor<ParkingSpot> spotCaptor;

    private ParkVehicleService service;

    @BeforeEach
    void setUp() {
        service = new ParkVehicleService(parkingSessionPort, parkingSpotPort, pricingCalculator);
    }

    @Nested
    @DisplayName("Input validation")
    class ValidacaoEntrada {

        @Test
        @DisplayName("licensePlate null -> ValidationException")
        void licensePlateNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processParked(null, LAT, LNG))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("licensePlate blank -> ValidationException")
        void licensePlateBlank_throwsValidationException() {
            assertThatThrownBy(() -> service.processParked("   ", LAT, LNG))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("licensePlate");
        }

        @Test
        @DisplayName("lat null -> ValidationException")
        void latNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, null, LNG))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("lat");
        }

        @Test
        @DisplayName("lng null -> ValidationException")
        void lngNull_throwsValidationException() {
            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, null))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("lng");
        }
    }

    @Nested
    @DisplayName("Business rules")
    class RegrasNegocio {

        @Test
        @DisplayName("Active session not found -> ActiveSessionNotFoundException")
        void sessaoAtivaNaoEncontrada_throwsActiveSessionNotFoundException() {
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, LNG))
                    .isInstanceOf(ActiveSessionNotFoundException.class)
                    .hasMessageContaining(LICENSE_PLATE);

            verify(parkingSpotPort, never()).findByLatAndLngWithLock(any(), any());
            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("Session with status other than ENTERED -> InvalidSessionTransitionException")
        void sessaoComStatusDiferenteDeEntered_throwsInvalidSessionTransitionException() {
            ParkingSession session = createEnteredSession();
            session.setStatus(ParkingSessionStatus.PARKED);

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, LNG))
                    .isInstanceOf(InvalidSessionTransitionException.class)
                    .hasMessageContaining("PARKED")
                    .hasMessageContaining("PARKED");

            verify(parkingSpotPort, never()).findByLatAndLngWithLock(any(), any());
            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("Spot not found at coordinates -> SpotNotFoundException")
        void spotNaoEncontrado_throwsSpotNotFoundException() {
            ParkingSession session = createEnteredSession();
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, LNG))
                    .isInstanceOf(SpotNotFoundException.class)
                    .hasMessageContaining(LAT.toString())
                    .hasMessageContaining(LNG.toString());

            verify(parkingSpotPort).findByLatAndLngWithLock(LAT, LNG);
            verify(parkingSpotPort, never()).save(any());
            verify(parkingSessionPort, never()).save(any());
        }

        @Test
        @DisplayName("Spot already occupied -> SpotAlreadyOccupiedException")
        void spotJaOcupado_throwsSpotAlreadyOccupiedException() {
            ParkingSession session = createEnteredSession();
            GarageSector sector = createSector();
            ParkingSpot spot = new ParkingSpot(1, sector, LAT, LNG, false);
            spot.occupy();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));

            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, LNG))
                    .isInstanceOf(SpotAlreadyOccupiedException.class)
                    .hasMessageContaining("already occupied");

            verify(parkingSpotPort, never()).save(any());
            verify(parkingSessionPort, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Success flow")
    class FluxoSucesso {

        @Test
        @DisplayName("Updates status to PARKED")
        void atualizaStatusParaParked() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processParked(LICENSE_PLATE, LAT, LNG);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.PARKED);
        }

        @Test
        @DisplayName("Associates spot to session")
        void associaSpotASessao() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getParkingSpot()).isSameAs(spot);
        }

        @Test
        @DisplayName("Associates sector to session via spot.getSector()")
        void associaSectorASessao() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getSector()).isSameAs(spot.getSector());
        }

        @Test
        @DisplayName("Sets parkedTime non-null, close to now")
        void defineParkedTime() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();
            Instant before = Instant.now();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            Instant after = Instant.now();
            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getParkedTime()).isNotNull();
            assertThat(captured.getParkedTime()).isBetween(before, after.plusSeconds(1));
        }

        @Test
        @DisplayName("Calcula e armazena hourlyPriceApplied corretamente")
        void calculaHourlyPriceAppliedCorretamente() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();
            BigDecimal expectedPrice = new BigDecimal("9.00");

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(
                    spot.getSector().getBasePrice(),
                    session.getPriceAdjustmentRateAtEntry()
            )).thenReturn(expectedPrice);
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured.getHourlyPriceApplied()).isEqualByComparingTo(expectedPrice);
        }

        @Test
        @DisplayName("Salva spot como ocupado")
        void salvaSpotComoOcupado() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(spotCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            ParkingSpot savedSpot = spotCaptor.getValue();
            assertThat(savedSpot.isOccupied()).isTrue();
        }

        @Test
        @DisplayName("Salva sessão atualizada via port")
        void salvaSessaoAtualizadaViaPort() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(sessionCaptor.capture())).thenAnswer(inv -> {
                ParkingSession s = inv.getArgument(0);
                s.setId(1L);
                return s;
            });

            service.processParked(LICENSE_PLATE, LAT, LNG);

            ParkingSession captured = sessionCaptor.getValue();
            assertThat(captured).isNotNull();
            assertThat(captured.getStatus()).isEqualTo(ParkingSessionStatus.PARKED);
            verify(parkingSessionPort).save(any(ParkingSession.class));
        }
    }

    @Nested
    @DisplayName("Interações")
    class Interacoes {

        @Test
        @DisplayName("Verifica que findActiveByLicensePlateWithLock é chamado")
        void verificaFindActiveByLicensePlateWithLockChamado() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            verify(parkingSessionPort).findActiveByLicensePlateWithLock(LICENSE_PLATE);
        }

        @Test
        @DisplayName("Verifica que findByLatAndLngWithLock é chamado com lat/lng corretos")
        void verificaFindByLatAndLngWithLockChamado() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            verify(parkingSpotPort).findByLatAndLngWithLock(LAT, LNG);
        }

        @Test
        @DisplayName("Verifica que parkingSpotPort.save é chamado")
        void verificaParkingSpotPortSaveChamado() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            verify(parkingSpotPort).save(any(ParkingSpot.class));
        }

        @Test
        @DisplayName("Verifica que parkingSessionPort.save é chamado")
        void verificaParkingSessionPortSaveChamado() {
            ParkingSession session = createEnteredSession();
            ParkingSpot spot = createSpot();

            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.of(session));
            when(parkingSpotPort.findByLatAndLngWithLock(LAT, LNG)).thenReturn(Optional.of(spot));
            when(pricingCalculator.calculateHourlyPrice(any(), any())).thenReturn(new BigDecimal("10.00"));
            when(parkingSpotPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(parkingSessionPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.processParked(LICENSE_PLATE, LAT, LNG);

            verify(parkingSessionPort).save(any(ParkingSession.class));
        }

        @Test
        @DisplayName("Não salva spot em cenários de erro")
        void naoSalvaSpotEmCenariosDeErro() {
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, LNG))
                    .isInstanceOf(ActiveSessionNotFoundException.class);

            verify(parkingSpotPort, never()).save(any());
        }

        @Test
        @DisplayName("Não salva session em cenários de erro")
        void naoSalvaSessionEmCenariosDeErro() {
            when(parkingSessionPort.findActiveByLicensePlateWithLock(LICENSE_PLATE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processParked(LICENSE_PLATE, LAT, LNG))
                    .isInstanceOf(ActiveSessionNotFoundException.class);

            verify(parkingSessionPort, never()).save(any());
        }
    }

    private ParkingSession createEnteredSession() {
        ParkingSession session = new ParkingSession(LICENSE_PLATE, Instant.parse("2025-03-13T10:00:00Z"), ParkingSessionStatus.ENTERED);
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

    private ParkingSpot createSpot() {
        return new ParkingSpot(1, createSector(), LAT, LNG, false);
    }
}
