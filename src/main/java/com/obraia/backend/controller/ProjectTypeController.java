package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.catalog.ProjectTypeResponse;
import com.obraia.backend.service.ProjectTypeService;

import io.swagger.v3.oas.annotations.Operation;
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

}
