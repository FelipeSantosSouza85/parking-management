package com.estapar.parking_management.webhook.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Unified DTO for webhook event payload.
 * Fields are nullable according to event type (e.g. entryTime only in ENTRY, lat/lng only in PARKED).
 * Dates are received as LocalDateTime (e.g. "2026-03-13T21:15:33") and converted to Instant (UTC) in services.
 */
public record WebhookEventRequest(
        String licensePlate,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        Double lat,
        Double lng,
        @NotNull(message = "eventType must not be null")
        WebhookEventType eventType
) {}
