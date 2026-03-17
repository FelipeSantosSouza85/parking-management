package com.estapar.parking_management.garage.application;

import com.estapar.parking_management.garage.infrastructure.client.GarageSimulatorClient.GarageSimulatorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@DisplayName("GarageInitializationService")
@ExtendWith(MockitoExtension.class)
class GarageInitializationServiceTest {

    @Mock
    private GarageSynchronizationService synchronizationService;

    private GarageInitializationService initializationService;

    @BeforeEach
    void setUp() {
        initializationService = new GarageInitializationService(synchronizationService);
    }

    @Test
    @DisplayName("deve sincronizar garagem no evento ApplicationReady")
    void shouldSynchronizeGarageOnApplicationReady() {
        // When
        initializationService.onApplicationReady();

        // Then
        verify(synchronizationService).synchronize();
    }

    @Test
    @DisplayName("deve continuar sem lançar exceção quando sincronização falhar")
    void shouldNotThrowWhenSynchronizationFails() {
        // Given
        doThrow(new GarageSimulatorException("Connection refused"))
                .when(synchronizationService).synchronize();

        // When/Then - não deve lançar exceção
        initializationService.onApplicationReady();

        verify(synchronizationService).synchronize();
    }
}
