package com.estapar.parking_management.webhook.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Event types supported by the external simulator webhook.
 */
@Schema(description = "Event types supported by the webhook")
public enum WebhookEventType {
    ENTRY,
    PARKED,
    EXIT
}
