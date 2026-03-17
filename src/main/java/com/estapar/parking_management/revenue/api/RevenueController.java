package com.estapar.parking_management.revenue.api;

import com.estapar.parking_management.revenue.api.dto.RevenueResponse;
import com.estapar.parking_management.revenue.application.RevenueService;
import com.estapar.parking_management.shared.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST controller that exposes the revenue query endpoint by sector and date.
 */
@RestController
@RequestMapping("/revenue")
@Validated
public class RevenueController {

    private static final Logger log = LoggerFactory.getLogger(RevenueController.class);

    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @Operation(
            summary = "Query revenue",
            description = "Returns aggregated revenue by sector and date"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revenue returned successfully",
                    content = @Content(schema = @Schema(implementation = RevenueResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sector not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<RevenueResponse> getRevenue(
            @Parameter(description = "Date in YYYY-MM-DD format", example = "2025-01-01")
            @RequestParam @NotNull LocalDate date,
            @Parameter(description = "Sector code", example = "A")
            @RequestParam @NotBlank String sector) {
        log.info("[REVENUE] - [QUERY] date={}, sector={}", date, sector);
        RevenueResponse response = revenueService.getRevenue(date, sector);
        return ResponseEntity.ok(response);
    }
}