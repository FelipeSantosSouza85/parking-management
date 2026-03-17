package com.estapar.parking_management.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO padrão para respostas de erro da API.
 */
@Schema(description = "Resposta de erro da API")
public record ApiErrorResponse(
        @Schema(description = "Código do erro", example = "GARAGE_FULL")
        String code,
        @Schema(description = "Mensagem descritiva do erro")
        String message,
        @Schema(description = "Timestamp do erro")
        Instant timestamp
) {
}
