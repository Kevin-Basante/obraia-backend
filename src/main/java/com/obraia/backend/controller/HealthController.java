package com.obraia.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.HealthResponse;
import com.obraia.backend.dto.HelloResponse;
import com.obraia.backend.service.HealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controllers translate HTTP into service calls and back:
 * they choose the status code and return the JSON body.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Server and database status")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    @Operation(summary = "Server and database status")
    @ApiResponse(responseCode = "200", description = "Server and database are working")
    @ApiResponse(responseCode = "503", description = "The database does not answer")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse report = healthService.getHealth();
        // 503 tells Render (and the frontend) that the API cannot reach the database.
        HttpStatus status = report.database().isConnected() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(report);
    }

    @GetMapping("/hello")
    @Operation(summary = "Hello world with data read from the database")
    @ApiResponse(responseCode = "200", description = "Message and database data")
    @ApiResponse(responseCode = "503", description = "The database does not answer",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public HelloResponse getHello() {
        // If the database fails, the exception reaches GlobalExceptionHandler.
        return healthService.getHello();
    }
}
