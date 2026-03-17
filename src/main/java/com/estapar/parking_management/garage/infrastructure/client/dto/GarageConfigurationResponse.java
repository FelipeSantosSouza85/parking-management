package com.estapar.parking_management.garage.infrastructure.client.dto;

import java.util.List;

/**
 * DTO raiz para representar a configuração completa da garagem retornada pelo simulador.
 */
public record GarageConfigurationResponse(
        List<SectorResponse> garage,
        List<SpotResponse> spots
) {
    public GarageConfigurationResponse {
        garage = garage != null ? List.copyOf(garage) : List.of();
        spots = spots != null ? List.copyOf(spots) : List.of();
    }
}
