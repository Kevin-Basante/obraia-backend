package com.obraia.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.ApiInfoResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Answers on the base URL so opening the domain shows something useful. */
@RestController
@Tag(name = "General", description = "API information")
public class RootController {

    @GetMapping("/")
    @Operation(summary = "General information about the API")
    public ApiInfoResponse getInfo() {
        return new ApiInfoResponse(
                "obraia-backend",
                "ObraIA REST API",
                "running",
                "/api/docs",
                "/api/health");
    }
}
