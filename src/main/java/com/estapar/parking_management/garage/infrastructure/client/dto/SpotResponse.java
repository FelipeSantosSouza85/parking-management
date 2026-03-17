package com.estapar.parking_management.garage.infrastructure.client.dto;

/**
 * DTO para representar uma vaga retornada pelo simulador.
 */
public record SpotResponse(
        Integer id,
        String sector,
        Double lat,
        Double lng,
        Boolean occupied
) {
}
