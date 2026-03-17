package com.estapar.parking_management.shared.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard DTO for API error responses.
 */
@Schema(description = "API error response")
public record ApiErrorResponse(
        @Schema(description = "Error code", example = "GARAGE_FULL")
        String code,
        @Schema(description = "Descriptive error message")
        String message,
        @Schema(description = "Error timestamp")
        Instant timestamp
) {
}
