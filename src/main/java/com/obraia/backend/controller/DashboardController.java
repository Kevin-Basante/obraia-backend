package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.overview.PortfolioResponse;
import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.service.ProjectOverviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Summaries for the dashboards, so the screens need a single request. */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Summary of all my projects and of one project")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DashboardController {

    private final ProjectOverviewService overviewService;

    public DashboardController(ProjectOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping
    @Operation(summary = "All my projects with their progress, money and risk",
            description = "Newest project first, with the totals of all of them on top.")
    @ApiResponse(responseCode = "200", description = "Totals and one card per project")
    public PortfolioResponse portfolio(@AuthenticationPrincipal Jwt jwt) {
        return overviewService.portfolio(ProjectController.userId(jwt));
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "Dashboard of one project",
            description = "Progress of the plan, next tasks, budget against expenses, latest risk and counts.")
    @ApiResponse(responseCode = "200", description = "Summary of the project")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectOverviewResponse project(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return overviewService.overview(projectId, ProjectController.userId(jwt));
    }
}
