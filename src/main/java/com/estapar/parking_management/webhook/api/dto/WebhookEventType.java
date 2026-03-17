package com.estapar.parking_management.webhook.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tipos de evento suportados pelo webhook do simulador externo.
 */
@Schema(description = "Tipos de evento suportados pelo webhook")
public enum WebhookEventType {
    ENTRY,
    PARKED,
    EXIT
}
