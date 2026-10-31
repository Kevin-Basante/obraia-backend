package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
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

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.dto.risk.RiskAssessmentResponse;
import com.obraia.backend.dto.risk.RiskOverviewResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.risk.RiskFactor;
import com.obraia.backend.service.RiskAssessmentService;

/** HTTP side of the risk routes, without the database. */
class RiskControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private RiskAssessmentService riskService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        riskService = mock(RiskAssessmentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RiskController(riskService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void overviewBeforeTheFirstAssessmentHasANullLatest() throws Exception {
        when(riskService.overview(projectId, userId)).thenReturn(new RiskOverviewResponse(null, List.of()));

        mvc.perform(get("/api/projects/{id}/risk", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latest").doesNotExist())
                .andExpect(jsonPath("$.history").isEmpty());
    }

    @Test
    void assessAnswers201WithTheFactors() throws Exception {
        when(riskService.assess(projectId, userId)).thenReturn(new RiskAssessmentResponse(UUID.randomUUID(),
                new StatusResponse("high", "High"), new BigDecimal("66.88"), 0,
                List.of(new RiskFactor("budget_overrun", "Budget overrun", new BigDecimal("75.00"),
                        new BigDecimal("0.25"), 0, "110% of the approved budget is spent")),
                "rules-v1", new StatusResponse("manual", "Requested by a member"),
                Instant.parse("2026-11-20T15:00:00Z")));

        mvc.perform(post("/api/projects/{id}/risk/assess", projectId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level.code").value("high"))
                .andExpect(jsonPath("$.factors[0].code").value("budget_overrun"))
                .andExpect(jsonPath("$.trigger.code").value("manual"));
    }

    @Test
    void unknownProjectIs404() throws Exception {
        when(riskService.assess(any(), any()))
                .thenThrow(new ResourceNotFoundException("PROJECT_NOT_FOUND", "The project does not exist"));

        mvc.perform(post("/api/projects/{id}/risk/assess", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_FOUND"));
    }
}
