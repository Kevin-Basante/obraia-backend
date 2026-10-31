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
import com.obraia.backend.dto.expense.ExpenseRequest;
import com.obraia.backend.dto.expense.ExpenseResponse;
import com.obraia.backend.dto.expense.ExpenseSummaryResponse;
import com.obraia.backend.service.ExpenseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** The money really spent on a project. */
@RestController
@RequestMapping("/api/projects/{projectId}/expenses")
@Tag(name = "Expenses", description = "Money really spent on the project, compared with the approved budget")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    @Operation(summary = "Expenses of the project, newest first")
    @ApiResponse(responseCode = "200", description = "Expenses; empty before the first one")
    @ApiResponse(responseCode = "404", description = "The project does not exist or the user is not a member",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<ExpenseResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return expenseService.list(projectId, ProjectController.userId(jwt));
    }

    @GetMapping("/summary")
    @Operation(summary = "Total spent by category, compared with the approved budget")
    @ApiResponse(responseCode = "200", description = "Totals; the budget fields are null without an approved budget")
    public ExpenseSummaryResponse summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId) {
        return expenseService.summary(projectId, ProjectController.userId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register an expense",
            description = "Categories: materials, labor, equipment, tax and other. The date is today when it is "
                    + "missing. The approved budget, if any, takes the new total spent.")
    @ApiResponse(responseCode = "201", description = "Expense registered")
    @ApiResponse(responseCode = "400", description = "Invalid fields, unknown category, future date or "
            + "a phase of another project", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ExpenseResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(projectId, ProjectController.userId(jwt), request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an expense registered by mistake")
    @ApiResponse(responseCode = "204", description = "Expense deleted")
    @ApiResponse(responseCode = "404", description = "The expense does not exist in the project",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The project is closed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID expenseId) {
        expenseService.delete(projectId, ProjectController.userId(jwt), expenseId);
    }
}
