package com.estapar.parking_management.shared.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller to trigger exceptions and validate GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/test/exceptions")
class ExceptionHandlerTestController {

    @GetMapping("/garage-full")
    void throwGarageFull() {
        throw new GarageFullException();
    }

    @GetMapping("/spot-already-occupied")
    void throwSpotAlreadyOccupied() {
        throw new SpotAlreadyOccupiedException();
    }

    @GetMapping("/active-session-already-exists/{licensePlate}")
    void throwActiveSessionAlreadyExists(@PathVariable String licensePlate) {
        throw new ActiveSessionAlreadyExistsException(licensePlate);
    }

    @GetMapping("/invalid-session-transition/{from}/{to}")
    void throwInvalidSessionTransition(@PathVariable String from, @PathVariable String to) {
        throw new InvalidSessionTransitionException(from, to);
    }

    @GetMapping("/spot-not-found/{lat}/{lng}")
    void throwSpotNotFound(@PathVariable Double lat, @PathVariable Double lng) {
        throw new SpotNotFoundException(lat, lng);
    }

    @GetMapping("/active-session-not-found/{licensePlate}")
    void throwActiveSessionNotFound(@PathVariable String licensePlate) {
        throw new ActiveSessionNotFoundException(licensePlate);
    }
}
