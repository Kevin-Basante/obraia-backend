package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.auth.TokenRequest;
import com.obraia.backend.dto.project.DeletionImpactResponse;
import com.obraia.backend.service.ProjectDeletionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Deleting a project: warnings, request by email and confirmation with the link. */
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project deletion", description = "Delete a project in two steps, confirmed by email")
public class ProjectDeletionController {

    private final ProjectDeletionService deletionService;

    public ProjectDeletionController(ProjectDeletionService deletionService) {
        this.deletionService = deletionService;
    }

    @GetMapping("/{id}/deletion-impact")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "What the project holds and would be lost")
    @ApiResponse(responseCode = "200", description = "Counts per module and purge date")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public DeletionImpactResponse impact(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return deletionService.impact(id, ProjectController.userId(jwt));
    }

    @PostMapping("/{id}/deletion-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Send the email that confirms the deletion")
    @ApiResponse(responseCode = "202", description = "Email sent")
    @ApiResponse(responseCode = "403", description = "Only the creator can delete the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "The email could not be sent",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse requestDeletion(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return deletionService.requestDeletion(id, ProjectController.userId(jwt));
    }

    @PostMapping("/deletion/confirm")
    @Operation(summary = "Confirm the deletion with the token from the email link")
    @ApiResponse(responseCode = "200", description = "Project deleted")
    @ApiResponse(responseCode = "400", description = "Invalid, used or expired link",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse confirmDeletion(@Valid @RequestBody TokenRequest request) {
        return deletionService.confirmDeletion(request.token());
    }
}
