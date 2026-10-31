package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.budget.BudgetResponse;
import com.obraia.backend.dto.budget.BudgetSummaryResponse;
import com.obraia.backend.dto.budget.QuotationResponse;
import com.obraia.backend.service.BudgetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The quotation of a project, saved as budget versions. */
@RestController
@RequestMapping("/api/projects/{projectId}/budgets")
@Tag(name = "Budgets", description = "Quotation from the measurements and budget versions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Budget versions of the project, newest first")
    @ApiResponse(responseCode = "200", description = "Versions without their rows; empty before the first quotation")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<BudgetSummaryResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return budgetService.list(projectId, ProjectController.userId(jwt));
    }

    @GetMapping("/preview")
    @Operation(summary = "Quote the project without saving",
            description = "Materials from the manual measurements, waste, transport of the city, "
                    + "labor for the working days of the plan and VAT on labor and transport.")
    @ApiResponse(responseCode = "200", description = "Cost layers, materials and the data used")
    @ApiResponse(responseCode = "409", description = "The project has no measurements or no formulas for them",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public QuotationResponse preview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return budgetService.preview(projectId, ProjectController.userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Quote the project and save it as a new draft version",
            description = "It also replaces the list of materials of the project.")
    @ApiResponse(responseCode = "201", description = "Draft version created with its rows")
    @ApiResponse(responseCode = "409", description = "The project is closed or has nothing to quote",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BudgetResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return budgetService.create(projectId, ProjectController.userId(jwt));
    }

    @GetMapping("/{budgetId}")
    @Operation(summary = "A budget version with its rows")
    @ApiResponse(responseCode = "200", description = "Version with one row per cost layer")
    @ApiResponse(responseCode = "404", description = "The budget does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BudgetResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID budgetId) {
        return budgetService.get(projectId, ProjectController.userId(jwt), budgetId);
    }

}
