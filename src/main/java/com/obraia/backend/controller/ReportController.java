package com.obraia.backend.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.auth.TokenRequest;
import com.obraia.backend.dto.report.FullReportAccessResponse;
import com.obraia.backend.dto.report.ShareLinkResponse;
import com.obraia.backend.dto.report.SharedReportRequest;
import com.obraia.backend.report.ReportFile;
import com.obraia.backend.service.ReportRequestService;
import com.obraia.backend.service.ReportService;
import com.obraia.backend.service.ReportShareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** The PDF reports of a project: public, full with the approval of the creator, and shared links. */
@RestController
@RequestMapping("/api")
@Tag(name = "Reports", description = "PDF reports of a project, with a summary written by the AI")
public class ReportController {

    private static final String LANG_HELP = "Language of the PDF: es or en (English when missing)";

    private final ReportService reportService;
    private final ReportRequestService requestService;
    private final ReportShareService shareService;

    public ReportController(ReportService reportService, ReportRequestService requestService,
            ReportShareService shareService) {
        this.reportService = reportService;
        this.requestService = requestService;
        this.shareService = shareService;
    }

    @GetMapping("/projects/{id}/reports/public")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Download the public report: progress, phases and next tasks")
    @ApiResponse(responseCode = "200", description = "The PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<byte[]> publicReport(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Parameter(description = LANG_HELP) @RequestParam(required = false) String lang) {
        return pdf(reportService.publicReport(id, ProjectController.userId(jwt), lang));
    }

    @GetMapping("/projects/{id}/reports/full")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Download the full report: the public one plus budget, expenses and risk")
    @ApiResponse(responseCode = "200", description = "The PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "403", description = "The creator has not approved the download, or it expired",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<byte[]> fullReport(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Parameter(description = LANG_HELP) @RequestParam(required = false) String lang) {
        return pdf(reportService.fullReport(id, ProjectController.userId(jwt), lang));
    }

    @GetMapping("/projects/{id}/reports/full/access")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Whether the user can download the full report, or where the request stands")
    @ApiResponse(responseCode = "200", description = "owner, none, pending, approved or expired")
    public FullReportAccessResponse fullAccess(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return requestService.access(id, ProjectController.userId(jwt));
    }

    @PostMapping("/projects/{id}/reports/full/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Ask the creator of the project to approve the full report")
    @ApiResponse(responseCode = "202", description = "Email sent to the creator")
    @ApiResponse(responseCode = "400", description = "The user can already download it",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "The email could not be sent",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse requestFull(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return requestService.request(id, ProjectController.userId(jwt));
    }

    @PostMapping("/projects/{id}/reports/share")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Create a link that opens the public report without an account")
    @ApiResponse(responseCode = "201", description = "The link and when it expires")
    public ShareLinkResponse share(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return shareService.create(id, ProjectController.userId(jwt));
    }

    @PostMapping("/reports/approve")
    @Operation(summary = "The creator approves the full report with the token of the email link")
    @ApiResponse(responseCode = "200", description = "Approved")
    @ApiResponse(responseCode = "400", description = "Invalid, used or expired link",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse approve(@Valid @RequestBody TokenRequest request) {
        return requestService.approve(request.token());
    }

    @PostMapping("/reports/shared")
    @Operation(summary = "Download the public report with the token of a shared link")
    @ApiResponse(responseCode = "200", description = "The PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "400", description = "Invalid or expired link",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<byte[]> shared(@Valid @RequestBody SharedReportRequest request) {
        return pdf(reportService.sharedReport(request.token(), request.lang()));
    }

    private static ResponseEntity<byte[]> pdf(ReportFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }
}
