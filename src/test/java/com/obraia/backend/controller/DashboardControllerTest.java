package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.obraia.backend.dto.overview.ActivityCounts;
import com.obraia.backend.dto.overview.MoneyOverview;
import com.obraia.backend.dto.overview.PortfolioResponse;
import com.obraia.backend.dto.overview.ProjectOverviewResponse;
import com.obraia.backend.dto.overview.WorkOverview;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.service.ProjectOverviewService;

/** HTTP side of the dashboard routes, without the database. */
class DashboardControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private ProjectOverviewService overviewService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        overviewService = mock(ProjectOverviewService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(overviewService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void portfolioAnswersTheTotals() throws Exception {
        when(overviewService.portfolio(userId)).thenReturn(new PortfolioResponse(2, 1, 0,
                new BigDecimal("1000000.00"), new BigDecimal("250000.00"), List.of()));

        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCount").value(2))
                .andExpect(jsonPath("$.active").value(1))
                .andExpect(jsonPath("$.totalSpent").value(250000.00))
                .andExpect(jsonPath("$.projects").isEmpty());
    }

    @Test
    void projectOverviewAnswersEveryPart() throws Exception {
        WorkOverview work = new WorkOverview(new BigDecimal("40.00"), LocalDate.of(2026, 11, 16),
                LocalDate.of(2027, 4, 16), 8, 1, 22, 3, 0, "Excavación", List.of(), List.of());
        MoneyOverview money = new MoneyOverview(null, 0, null, BigDecimal.ZERO, null, null, 0, List.of());
        when(overviewService.overview(projectId, userId)).thenReturn(
                new ProjectOverviewResponse(projectId, work, money, null, new ActivityCounts(1, 10, 6, 2)));

        mvc.perform(get("/api/dashboard/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.work.currentPhase").value("Excavación"))
                .andExpect(jsonPath("$.work.taskCount").value(22))
                .andExpect(jsonPath("$.money.approvedBudget").doesNotExist())
                .andExpect(jsonPath("$.risk").doesNotExist())
                .andExpect(jsonPath("$.counts.aiMeasurements").value(6));
    }

    @Test
    void projectOfSomeoneElseIs404() throws Exception {
        when(overviewService.overview(projectId, userId))
                .thenThrow(new ResourceNotFoundException("PROJECT_NOT_FOUND", "The project does not exist"));

        mvc.perform(get("/api/dashboard/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_FOUND"));
    }
}
