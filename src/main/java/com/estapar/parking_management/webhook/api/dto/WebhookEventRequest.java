package com.estapar.parking_management.webhook.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO unificado para o payload de eventos do webhook.
 * Campos nullable conforme o tipo de evento (ex: entryTime só vem no ENTRY, lat/lng só no PARKED).
 * Datas são recebidas como LocalDateTime (ex: "2026-03-13T21:15:33") e convertidas para Instant (UTC) nos services.
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
