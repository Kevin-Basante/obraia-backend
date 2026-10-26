package com.obraia.backend.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.blueprint.BlueprintResponse;
import com.obraia.backend.service.BlueprintService;
import com.obraia.backend.service.BlueprintService.BlueprintFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Blueprints (photos or PDFs) of a project. */
@RestController
@RequestMapping("/api/projects/{projectId}/blueprints")
@Tag(name = "Blueprints", description = "Upload, open and delete the blueprints of a project")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BlueprintController {

    private final BlueprintService blueprintService;

    public BlueprintController(BlueprintService blueprintService) {
        this.blueprintService = blueprintService;
    }

    @GetMapping
    @Operation(summary = "Blueprints of the project, newest first")
    @ApiResponse(responseCode = "200", description = "Blueprints")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<BlueprintResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return blueprintService.list(projectId, ProjectController.userId(jwt));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a blueprint", description = "A PDF or an image (PNG, JPG or WEBP) up to 10 MB, "
            + "sent as multipart/form-data in the part \"file\". A project can have up to 10 blueprints.")
    @ApiResponse(responseCode = "201", description = "Blueprint uploaded")
    @ApiResponse(responseCode = "400", description = "Missing, empty or unsupported file",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed or already has 10 blueprints",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "The file is larger than 10 MB",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BlueprintResponse upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file) throws IOException {
        return blueprintService.upload(projectId, ProjectController.userId(jwt), file.getOriginalFilename(),
                file.getBytes());
    }

    @GetMapping("/{blueprintId}/file")
    @Operation(summary = "Open the file of a blueprint",
            description = "Answers the bytes with their real content type, to show them in the browser.")
    @ApiResponse(responseCode = "200", description = "The file")
    @ApiResponse(responseCode = "404", description = "The blueprint does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<byte[]> file(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID blueprintId) {
        BlueprintFile file = blueprintService.download(projectId, ProjectController.userId(jwt), blueprintId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(file.content());
    }

}
