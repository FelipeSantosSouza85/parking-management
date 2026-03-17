package com.estapar.parking_management.webhook.api.dto;

import java.time.Instant;

/**
 * Success response DTO for webhook events.
 */
public record WebhookEventResponse(
        String message,
        Instant timestamp
) {}
