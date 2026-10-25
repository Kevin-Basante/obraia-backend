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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.project.AddMemberRequest;
import com.obraia.backend.dto.project.MemberResponse;
import com.obraia.backend.service.ProjectMemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Users who manage a project together. */
@RestController
@RequestMapping("/api/projects/{projectId}/members")
@Tag(name = "Project members", description = "Share a project with other registered users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    public ProjectMemberController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "Members of the project, in the order they joined")
    @ApiResponse(responseCode = "200", description = "Members")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<MemberResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return memberService.list(projectId, ProjectController.userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a registered user by email")
    @ApiResponse(responseCode = "201", description = "Member added")
    @ApiResponse(responseCode = "404", description = "No active account uses that email",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The user is already a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MemberResponse add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @Valid @RequestBody AddMemberRequest request) {
        return memberService.add(projectId, ProjectController.userId(jwt), request.email());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member from the project")
    @ApiResponse(responseCode = "204", description = "Member removed")
    @ApiResponse(responseCode = "409", description = "The creator cannot be removed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void remove(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId, @PathVariable UUID userId) {
        memberService.remove(projectId, ProjectController.userId(jwt), userId);
    }
}
