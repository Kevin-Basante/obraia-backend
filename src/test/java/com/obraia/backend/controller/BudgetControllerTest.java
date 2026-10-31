package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.budget.BudgetItemResponse;
import com.obraia.backend.dto.budget.BudgetResponse;
import com.obraia.backend.dto.budget.ProjectMaterialResponse;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.BudgetService;

/** HTTP side of the budget and material routes, without the database. */
class BudgetControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID budgetId = UUID.randomUUID();

    private BudgetService budgetService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        budgetService = mock(BudgetService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new BudgetController(budgetService), new ProjectMaterialController(budgetService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAnswers201WithTheRows() throws Exception {
        when(budgetService.create(projectId, userId)).thenReturn(budget("draft", "Draft"));

        mvc.perform(post("/api/projects/{id}/budgets", projectId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status.code").value("draft"))
                .andExpect(jsonPath("$.items[0].itemType").value("transport"))
                .andExpect(jsonPath("$.items[0].percentage").value(3.0));
    }

    @Test
    void projectWithoutMeasurementsAnswers409() throws Exception {
        when(budgetService.preview(projectId, userId))
                .thenThrow(new ConflictException("NO_MEASUREMENTS", "Add the measurements first"));

        mvc.perform(get("/api/projects/{id}/budgets/preview", projectId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NO_MEASUREMENTS"));
    }

    @Test
    void approveAnswers200() throws Exception {
        when(budgetService.approve(projectId, userId, budgetId)).thenReturn(budget("approved", "Approved"));

        mvc.perform(post("/api/projects/{id}/budgets/{budgetId}/approve", projectId, budgetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.label").value("Approved"));
    }

    @Test
    void materialsAreListedUnderTheProject() throws Exception {
        when(budgetService.materials(projectId, userId)).thenReturn(List.of(new ProjectMaterialResponse(
                UUID.randomUUID(), "Gravilla", "m3", new BigDecimal("2.10"), new BigDecimal("115000.00"),
                new BigDecimal("241500.00"))));

        mvc.perform(get("/api/projects/{id}/materials", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Gravilla"))
                .andExpect(jsonPath("$[0].subtotal").value(241500.0));
    }

    private BudgetResponse budget(String code, String label) {
        return new BudgetResponse(budgetId, 1, new StatusResponse(code, label), new BigDecimal("7119.00"),
                BigDecimal.ZERO, Instant.parse("2026-11-02T13:00:00Z"), null,
                List.of(new BudgetItemResponse("transport", "Transport", "Transport of the materials to the site",
                        new BigDecimal("7119.00"), new BigDecimal("3.00"))));
    }
}
