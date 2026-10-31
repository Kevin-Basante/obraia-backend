package com.obraia.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.budget.ProjectMaterialResponse;
import com.obraia.backend.service.BudgetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** The list of materials to buy, from the last quotation of the project. */
@RestController
@RequestMapping("/api/projects/{projectId}/materials")
@Tag(name = "Budgets")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProjectMaterialController {

    private final BudgetService budgetService;

    public ProjectMaterialController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    @Operation(summary = "Materials of the last quotation, waste included",
            description = "Empty until the project is quoted with POST /budgets.")
    @ApiResponse(responseCode = "200", description = "Quantity, price and subtotal of each material")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<ProjectMaterialResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return budgetService.materials(projectId, ProjectController.userId(jwt));
    }
}
