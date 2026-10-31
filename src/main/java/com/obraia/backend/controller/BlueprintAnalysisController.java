package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.blueprint.BlueprintAnalysisResponse;
import com.obraia.backend.service.BlueprintAnalysisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Reading a blueprint with the AI. */
@RestController
@RequestMapping("/api/projects/{projectId}/blueprints/{blueprintId}")
@Tag(name = "Blueprints", description = "Upload, open and delete the blueprints of a project")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BlueprintAnalysisController {

    private final BlueprintAnalysisService analysisService;

    public BlueprintAnalysisController(BlueprintAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Read the blueprint with the AI",
            description = "Sends the blueprint to the AI service and saves the elements it finds as measurements "
                    + "with source \"ocr\", replacing the ones it read before. The same file is not sent twice: "
                    + "the second time the answer comes from the cache. The first call can take about a minute "
                    + "while the AI service wakes up.")
    @ApiResponse(responseCode = "200", description = "Blueprint read; the measurements were saved")
    @ApiResponse(responseCode = "404", description = "The blueprint does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "The project used all its analyses for this hour",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "The AI service failed; the blueprint is marked as failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "The AI service is not configured",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BlueprintAnalysisResponse analyze(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID blueprintId) {
        return analysisService.analyze(projectId, ProjectController.userId(jwt), blueprintId);
    }
}
