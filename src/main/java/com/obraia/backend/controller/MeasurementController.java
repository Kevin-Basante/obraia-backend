package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.blueprint.ComparisonResponse;
import com.obraia.backend.dto.blueprint.MeasurementRequest;
import com.obraia.backend.dto.blueprint.MeasurementResponse;
import com.obraia.backend.service.MeasurementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Measurements of the building elements of a project. */
@RestController
@RequestMapping("/api/projects/{projectId}/measurements")
@Tag(name = "Measurements", description = "Manual measurements and their comparison with the AI")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping
    @Operation(summary = "Measurements in construction order, then by floor")
    @ApiResponse(responseCode = "200", description = "Measurements with their amount in m3 or m2")
    @ApiResponse(responseCode = "400", description = "Unknown element type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<MeasurementResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @Parameter(description = "1 or 2", example = "1") @RequestParam(required = false) Integer floor,
            @Parameter(description = "foundation, column, beam, slab, wall, floor or roof", example = "wall")
            @RequestParam(required = false) String element) {
        return measurementService.list(projectId, ProjectController.userId(jwt), floor, element);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a manual measurement",
            description = "The dimensions needed depend on the element; see /api/catalogs/element-types.")
    @ApiResponse(responseCode = "201", description = "Measurement added")
    @ApiResponse(responseCode = "400", description = "Invalid fields, a missing dimension or a floor the project "
            + "does not have", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MeasurementResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @Valid @RequestBody MeasurementRequest request) {
        return measurementService.create(projectId, ProjectController.userId(jwt), request);
    }

    @PutMapping("/{measurementId}")
    @Operation(summary = "Edit a manual measurement")
    @ApiResponse(responseCode = "200", description = "Measurement saved")
    @ApiResponse(responseCode = "400", description = "Invalid fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "The measurement does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed or the measurement comes from the AI",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MeasurementResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID measurementId, @Valid @RequestBody MeasurementRequest request) {
        return measurementService.update(projectId, ProjectController.userId(jwt), measurementId, request);
    }

    @DeleteMapping("/{measurementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a manual measurement")
    @ApiResponse(responseCode = "204", description = "Measurement deleted")
    @ApiResponse(responseCode = "404", description = "The measurement does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed or the measurement comes from the AI",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID measurementId) {
        measurementService.delete(projectId, ProjectController.userId(jwt), measurementId);
    }

    @GetMapping("/comparison")
    @Operation(summary = "Manual totals against AI totals, by element and floor",
            description = "Until the AI measures a blueprint, every row is manual_only.")
    @ApiResponse(responseCode = "200", description = "Totals and their difference in percent")
    public ComparisonResponse comparison(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return measurementService.compare(projectId, ProjectController.userId(jwt));
    }
}
