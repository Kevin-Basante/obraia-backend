package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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

import com.obraia.backend.ai.AiLimitReachedException;
import com.obraia.backend.ai.AiServiceBusyException;
import com.obraia.backend.ai.AiServiceDisabledException;
import com.obraia.backend.ai.AiServiceException;
import com.obraia.backend.dto.blueprint.BlueprintAnalysisResponse;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.BlueprintAnalysisService;

/** HTTP side of the blueprint analysis, without the database or the AI. */
class BlueprintAnalysisControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID blueprintId = UUID.randomUUID();

    private BlueprintAnalysisService analysisService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        analysisService = mock(BlueprintAnalysisService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BlueprintAnalysisController(analysisService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void analyzeAnswersTheSavedMeasurements() throws Exception {
        when(analysisService.analyze(projectId, userId, blueprintId)).thenReturn(new BlueprintAnalysisResponse(
                blueprintId, new StatusResponse("completed", "Completed"), "gemini-test", true, 1,
                List.of("Escala no visible"), List.of()));

        mvc.perform(post("/api/projects/{id}/blueprints/{blueprintId}/analyze", projectId, blueprintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processingStatus.code").value("completed"))
                .andExpect(jsonPath("$.cached").value(true))
                .andExpect(jsonPath("$.discarded").value(1))
                .andExpect(jsonPath("$.warnings[0]").value("Escala no visible"));
    }

    @Test
    void failedAiAnswers502() throws Exception {
        when(analysisService.analyze(projectId, userId, blueprintId))
                .thenThrow(new AiServiceException("The AI service could not be reached or did not answer in time", null));

        mvc.perform(post("/api/projects/{id}/blueprints/{blueprintId}/analyze", projectId, blueprintId))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("AI_SERVICE_FAILED"));
    }

    @Test
    void busyAiAnswers503() throws Exception {
        when(analysisService.analyze(projectId, userId, blueprintId)).thenThrow(new AiServiceBusyException(null));

        mvc.perform(post("/api/projects/{id}/blueprints/{blueprintId}/analyze", projectId, blueprintId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("AI_SERVICE_BUSY"))
                .andExpect(jsonPath("$.error.message").value(AiServiceBusyException.MESSAGE));
    }

    @Test
    void limitReachedAnswers429() throws Exception {
        when(analysisService.analyze(projectId, userId, blueprintId))
                .thenThrow(new AiLimitReachedException(5, Duration.ofHours(1)));

        mvc.perform(post("/api/projects/{id}/blueprints/{blueprintId}/analyze", projectId, blueprintId))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("AI_LIMIT_REACHED"))
                .andExpect(jsonPath("$.error.message")
                        .value("A project can analyze up to 5 blueprints every 60 minutes. Try again later"));
    }

    @Test
    void disabledAiAnswers503() throws Exception {
        when(analysisService.analyze(projectId, userId, blueprintId)).thenThrow(new AiServiceDisabledException());

        mvc.perform(post("/api/projects/{id}/blueprints/{blueprintId}/analyze", projectId, blueprintId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("AI_SERVICE_DISABLED"));
    }
}
