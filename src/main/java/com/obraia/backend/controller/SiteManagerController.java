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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.project.SiteManagerRequest;
import com.obraia.backend.dto.project.SiteManagerResponse;
import com.obraia.backend.service.SiteManagerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** The site managers each user saves to assign them to projects. */
@RestController
@RequestMapping("/api/site-managers")
@Tag(name = "Site managers", description = "Contacts of the people in charge of the work on site")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class SiteManagerController {

    private final SiteManagerService siteManagerService;

    public SiteManagerController(SiteManagerService siteManagerService) {
        this.siteManagerService = siteManagerService;
    }

    @GetMapping
    @Operation(summary = "My site managers, by name")
    @ApiResponse(responseCode = "200", description = "Site managers")
    public List<SiteManagerResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return siteManagerService.list(ProjectController.userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save a site manager")
    @ApiResponse(responseCode = "201", description = "Site manager saved")
    @ApiResponse(responseCode = "400", description = "Invalid fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SiteManagerResponse create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SiteManagerRequest request) {
        return siteManagerService.create(ProjectController.userId(jwt), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a site manager")
    @ApiResponse(responseCode = "200", description = "Site manager updated")
    @ApiResponse(responseCode = "404", description = "The site manager does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SiteManagerResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody SiteManagerRequest request) {
        return siteManagerService.update(id, ProjectController.userId(jwt), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a site manager", description = "Its projects are left without a site manager.")
    @ApiResponse(responseCode = "204", description = "Site manager deleted")
    @ApiResponse(responseCode = "404", description = "The site manager does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        siteManagerService.delete(id, ProjectController.userId(jwt));
    }
}
