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
        @DisplayName("should synchronize garage on ApplicationReady event")
    void shouldSynchronizeGarageOnApplicationReady() {
        // When
        initializationService.onApplicationReady();

        // Then
        verify(synchronizationService).synchronize();
    }

    @Test
        @DisplayName("should continue without throwing when synchronization fails")
    void shouldNotThrowWhenSynchronizationFails() {
        // Given
        doThrow(new GarageSimulatorException("Connection refused"))
                .when(synchronizationService).synchronize();

        // When/Then - should not throw exception
        initializationService.onApplicationReady();

        verify(synchronizationService).synchronize();
    }
}
