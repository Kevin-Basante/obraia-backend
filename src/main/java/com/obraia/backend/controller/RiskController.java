package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.dto.risk.RiskOverviewResponse;
import com.obraia.backend.service.RiskAssessmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The risk of delay and of going over budget. */
@RestController
@RequestMapping("/api/projects/{projectId}/risk")
@Tag(name = "Risk", description = "Risk of delay and of going over budget, with its history")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class RiskController {

    private final RiskAssessmentService riskService;

    public RiskController(RiskAssessmentService riskService) {
        this.riskService = riskService;
    }

    @GetMapping
    @Operation(summary = "Latest risk assessment and the history of the score",
            description = "A new assessment is made after every progress report and every expense.")
    @ApiResponse(responseCode = "200", description = "Latest assessment, null before the first one, and history")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RiskOverviewResponse overview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return riskService.overview(projectId, ProjectController.userId(jwt));
    }

    @PostMapping("/assess")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assess the risk now",
            description = "Four rules score the project from 0 to 100: late tasks, progress against time, "
                    + "budget spent and spending against progress.")
    @ApiResponse(responseCode = "201", description = "New assessment with its factors")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public RiskAssessmentResponse assess(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return riskService.assess(projectId, ProjectController.userId(jwt));
    }
}
