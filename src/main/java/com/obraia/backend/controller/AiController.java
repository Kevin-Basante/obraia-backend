package com.obraia.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ai.AiWakeResponse;
import com.obraia.backend.service.AiWakeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Wakes the AI service before a page uses it. */
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI", description = "State of the AI service")
public class AiController {

    private final AiWakeService wakeService;

    public AiController(AiWakeService wakeService) {
        this.wakeService = wakeService;
    }

    @GetMapping("/wake")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Wake the AI service in the background; it answers at once")
    @ApiResponse(responseCode = "200", description = "enabled is false when the AI service is not configured")
    public AiWakeResponse wake() {
        return wakeService.wake();
    }
}
