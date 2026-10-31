package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.phase.ProgressReportRequest;
import com.obraia.backend.dto.phase.WorkPlanResponse;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.PhaseService;
import com.obraia.backend.service.ProgressReportService;
import com.obraia.backend.service.TaskService;

/** HTTP side of the phase, task and progress routes, without the database. */
class PhaseControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private PhaseService phaseService;
    private TaskService taskService;
    private ProgressReportService reportService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        phaseService = mock(PhaseService.class);
        taskService = mock(TaskService.class);
        reportService = mock(ProgressReportService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new PhaseController(phaseService), new TaskController(taskService, reportService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateAnswers201() throws Exception {
        when(phaseService.generate(projectId, userId)).thenReturn(WorkPlanResponse.of(List.of()));

        mvc.perform(post("/api/projects/{id}/phases/generate", projectId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressPct").value(0))
                .andExpect(jsonPath("$.phases").isEmpty());
    }

    @Test
    void secondPlanAnswers409() throws Exception {
        when(phaseService.generate(projectId, userId))
                .thenThrow(new ConflictException("PHASES_ALREADY_CREATED", "The project already has phases"));

        mvc.perform(post("/api/projects/{id}/phases/generate", projectId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PHASES_ALREADY_CREATED"));
    }

    @Test
    void deletingThePlanAnswers204() throws Exception {
        mvc.perform(delete("/api/projects/{id}/phases", projectId))
                .andExpect(status().isNoContent());

        verify(phaseService).reset(projectId, userId);
    }

    @Test
    void phaseDatesOutOfOrderAnswer400() throws Exception {
        String body = """
                {"name": "Cimentación", "plannedStartDate": "2026-11-20", "plannedEndDate": "2026-11-10"}
                """;

        mvc.perform(put("/api/projects/{id}/phases/{pid}", projectId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.plannedDatesInOrder").exists());
        verifyNoInteractions(phaseService);
    }

    @Test
    void taskNeedsAPhaseAndAName() throws Exception {
        mvc.perform(post("/api/projects/{id}/tasks", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.phaseId").exists())
                .andExpect(jsonPath("$.error.fields.name").exists());
    }

    @Test
    void statusChangeNeedsAStatus() throws Exception {
        mvc.perform(patch("/api/projects/{id}/tasks/{tid}/status", projectId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.status").exists());
    }

    @Test
    void percentageAbove100Answers400() throws Exception {
        mvc.perform(post("/api/projects/{id}/tasks/{tid}/progress", projectId, UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("{\"completionPercentage\": 100.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.completionPercentage").exists());
        verifyNoInteractions(reportService);
    }

    @Test
    void fromSiteManagerIsOptional() throws Exception {
        UUID taskId = UUID.randomUUID();

        mvc.perform(post("/api/projects/{id}/tasks/{tid}/progress", projectId, taskId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"completionPercentage\": 35}"))
                .andExpect(status().isCreated());

        verify(reportService).create(eq(projectId), eq(userId), eq(taskId),
                eq(new ProgressReportRequest(new BigDecimal("35"), null, null, null)));
    }

    @Test
    void reportsAreListed() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(reportService.list(any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/projects/{id}/tasks/{tid}/progress", projectId, taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
