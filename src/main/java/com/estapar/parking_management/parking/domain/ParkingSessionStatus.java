package com.estapar.parking_management.parking.domain;

/**
 * Status do ciclo de vida de uma sessão de estacionamento.
 */
public enum ParkingSessionStatus {
    /** Veículo entrou na garagem */
    ENTERED,
    /** Veículo estacionou em uma vaga */
    PARKED,
    /** Veículo saiu da garagem */
    EXITED,
    /** Veículo rejeitado na entrada (garagem cheia) */
    REJECTED
}
