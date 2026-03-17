package com.estapar.parking_management.garage.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Service responsible for initializing garage data at application startup.
 */
@Component
public class GarageInitializationService {

    private static final Logger log = LoggerFactory.getLogger(GarageInitializationService.class);

    private final GarageSynchronizationService synchronizationService;

    public GarageInitializationService(GarageSynchronizationService synchronizationService) {
        this.synchronizationService = synchronizationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[GARAGE] - [INIT_START]");

        try {
            synchronizationService.synchronize();
            log.info("[GARAGE] - [INIT_OK]");
        } catch (Exception e) {
            log.error("[GARAGE] - [INIT_FAIL] {}. Application will continue without garage data.",
                    e.getMessage(), e);
        }
    }
}
