package com.estapar.parking_management.webhook.api.dto;

import java.time.Instant;

/**
 * DTO de resposta de sucesso para eventos do webhook.
 */
public record WebhookEventResponse(
        String message,
        Instant timestamp
) {}
