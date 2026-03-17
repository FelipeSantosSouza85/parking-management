package com.estapar.parking_management.garage.infrastructure.client.dto;

/**
 * DTO representing a spot returned by the simulator.
 */
public record SpotResponse(
        Integer id,
        String sector,
        Double lat,
        Double lng,
        Boolean occupied
) {
}
