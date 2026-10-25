package com.obraia.backend.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.catalog.MaterialFormulaResponse;
import com.obraia.backend.dto.catalog.PhasePlanResponse;
import com.obraia.backend.dto.catalog.ProjectTypeResponse;
import com.obraia.backend.service.ProjectTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Public catalog of project types. Guests can read it without signing in. */
@RestController
@RequestMapping("/api/catalogs/project-types")
@Tag(name = "Catalogs", description = "Project types, cities and materials (read only, public)")
public class ProjectTypeController {

    private final ProjectTypeService projectTypeService;

    public ProjectTypeController(ProjectTypeService projectTypeService) {
        this.projectTypeService = projectTypeService;
    }

    @GetMapping
    @Operation(summary = "List the project types")
    public ResponseEntity<List<ProjectTypeResponse>> list() {
        return CatalogResponses.cached(projectTypeService.listProjectTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "One project type")
    @ApiResponse(responseCode = "200", description = "The project type")
    @ApiResponse(responseCode = "404", description = "The project type does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ProjectTypeResponse> get(@PathVariable UUID id) {
        return CatalogResponses.cached(projectTypeService.getProjectType(id));
    }

    @GetMapping("/{id}/phases")
    @Operation(summary = "Standard phases of a project type",
            description = "Send the built area to choose the size automatically (up to 120 m2 is small).")
    @ApiResponse(responseCode = "200", description = "Phases in construction order and total duration")
    @ApiResponse(responseCode = "400", description = "Unknown size or invalid area",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "The project type does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PhasePlanResponse> phases(@PathVariable UUID id,
            @Parameter(description = "small or large", example = "small")
            @RequestParam(required = false) String size,
            @Parameter(description = "Built area in m2; when present, it decides the size", example = "95")
            @RequestParam(required = false) BigDecimal area) {
        return CatalogResponses.cached(projectTypeService.getPhasePlan(id, size, area));
    }

    @GetMapping("/{id}/material-formulas")
    @Operation(summary = "Material consumption per building element")
    @ApiResponse(responseCode = "200", description = "Formulas in construction order")
    @ApiResponse(responseCode = "400", description = "Unknown element type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "The project type does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<MaterialFormulaResponse>> materialFormulas(@PathVariable UUID id,
            @Parameter(description = "foundation, column, beam, slab, wall, floor or roof", example = "wall")
            @RequestParam(required = false) String element) {
        return CatalogResponses.cached(projectTypeService.getMaterialFormulas(id, element));
    }
}
