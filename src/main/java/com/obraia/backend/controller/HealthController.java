package com.obraia.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.HealthResponse;
import com.obraia.backend.dto.HelloResponse;
import com.obraia.backend.service.HealthService;

/**
 * Controllers translate HTTP into service calls and back:
 * they choose the status code and return the JSON body.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse report = healthService.getHealth();
        // 503 tells Render (and the frontend) that the API cannot reach the database.
        HttpStatus status = report.database().isConnected() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(report);
    }

    @GetMapping("/hello")
    public HelloResponse getHello() {
        // If the database fails, the exception reaches GlobalExceptionHandler.
        return healthService.getHello();
    }
}
