package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.phase.PlanPreviewResponse;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.service.PhaseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The work plan of a project: its phases, each one with its tasks. */
@RestController
@RequestMapping("/api/projects/{projectId}/phases")
@Tag(name = "Phases", description = "Work plan created from the standard phases of the project type")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PhaseController {

    private final PhaseService phaseService;

    public PhaseController(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @GetMapping
    @Operation(summary = "Phases with their tasks and progress",
            description = "The list is empty until the plan is created with POST /generate.")
    @ApiResponse(responseCode = "200", description = "Plan with the progress of the project, phases and tasks")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public WorkPlanResponse plan(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return phaseService.plan(projectId, ProjectController.userId(jwt));
    }

    @GetMapping("/preview")
    @Operation(summary = "Phases the plan would have, without saving them",
            description = "Durations are working days (Monday to Saturday) counted from the planned start.")
    @ApiResponse(responseCode = "200", description = "Proposed phases with their dates and starting tasks")
    @ApiResponse(responseCode = "409", description = "There is no planner for the project type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PlanPreviewResponse preview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return phaseService.preview(projectId, ProjectController.userId(jwt));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the plan from the standard phases")
    @ApiResponse(responseCode = "201", description = "Phases and tasks created")
    @ApiResponse(responseCode = "409", description = "The project is closed or already has phases",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public WorkPlanResponse generate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return phaseService.generate(projectId, ProjectController.userId(jwt));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the plan to create it again",
            description = "Only while no task has progress reports.")
    @ApiResponse(responseCode = "204", description = "Phases and tasks deleted")
    @ApiResponse(responseCode = "409", description = "The project is closed or the plan has progress",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void reset(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        phaseService.reset(projectId, ProjectController.userId(jwt));
    }

}
