package com.estapar.parking_management.webhook.api;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.estapar.parking_management.parking.application.EntryVehicleService;
import com.estapar.parking_management.parking.application.ExitVehicleService;
import com.estapar.parking_management.parking.application.ParkVehicleService;
import com.estapar.parking_management.shared.exception.ApiErrorResponse;
import com.estapar.parking_management.webhook.api.dto.WebhookEventRequest;
import com.estapar.parking_management.webhook.api.dto.WebhookEventResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

/**
 * REST controller that receives events from the external simulator via webhook.
 */
@RestController
public class WebhookController {

    private static final String MSG_ENTRY = "Vehicle entry registered successfully";
    private static final String MSG_PARKED = "Vehicle parked successfully";
    private static final String MSG_EXIT = "Vehicle exit processed successfully";

    private final EntryVehicleService entryVehicleService;
    private final ParkVehicleService parkVehicleService;
    private final ExitVehicleService exitVehicleService;

    public WebhookController(
            EntryVehicleService entryVehicleService,
            ParkVehicleService parkVehicleService,
            ExitVehicleService exitVehicleService
    ) {
        this.entryVehicleService = entryVehicleService;
        this.parkVehicleService = parkVehicleService;
        this.exitVehicleService = exitVehicleService;
    }

    @Operation(
            summary = "Receive webhook event",
            description = "Processes ENTRY (vehicle entry), PARKED (parking confirmation) and EXIT (vehicle exit) events sent by the external simulator."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event processed successfully",
                    content = @Content(schema = @Schema(implementation = WebhookEventResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Business rule violated",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/webhook")
    public ResponseEntity<WebhookEventResponse> handleEvent(@RequestBody @Valid WebhookEventRequest request) {
        
        String message = null;
        
        switch (request.eventType()) {
            case ENTRY -> {
                entryVehicleService.processEntry(request.licensePlate(), request.entryTime());
                message = MSG_ENTRY;
            }
            case PARKED -> {
                parkVehicleService.processParked(request.licensePlate(), request.lat(), request.lng());
                message =  MSG_PARKED;
            }
            case EXIT -> {
                exitVehicleService.processExit(request.licensePlate(), request.exitTime());
                message =  MSG_EXIT;
            }
        }
        return ResponseEntity.ok(new WebhookEventResponse(message, Instant.now()));
    }
}
