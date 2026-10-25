package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import com.obraia.backend.dto.project.ProjectRequest;
import com.obraia.backend.dto.project.ProjectResponse;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.service.ProjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Projects of the signed-in user. Users only reach the projects they are members of. */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Create, list, edit and follow the status of projects")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a project in draft status")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "400", description = "Invalid fields, project type or city",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProjectRequest request) {
        return projectService.create(userId(jwt), request);
    }

    @GetMapping
    @Operation(summary = "List my projects, newest first",
            description = "Optional filters: status code and a search on the name or city that ignores accents.")
    @ApiResponse(responseCode = "200", description = "Projects of the user")
    @ApiResponse(responseCode = "400", description = "Unknown status",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<ProjectSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(required = false) String search) {
        return projectService.list(userId(jwt), status, search);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full data of one project")
    @ApiResponse(responseCode = "200", description = "Project data")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return projectService.get(id, userId(jwt));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit the data of a project")
    @ApiResponse(responseCode = "200", description = "Project updated")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is completed or cancelled",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, userId(jwt), request);
    }

    static UUID userId(Jwt jwt) {
        // The "sub" claim of the token holds the user id.
        return UUID.fromString(jwt.getSubject());
    }
}
