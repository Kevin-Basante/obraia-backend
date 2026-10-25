package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.ProjectDeletionService;
import com.obraia.backend.service.ProjectService;
import com.obraia.backend.service.ProjectSiteManagerService;

/**
 * Checks the HTTP side of the project routes (status codes, validation
 * and error JSON) with a signed-in user and without the database.
 */
class ProjectControllerTest {

    private final UUID userId = UUID.randomUUID();

    private ProjectService projectService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        projectService = mock(ProjectService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new ProjectController(projectService, mock(ProjectSiteManagerService.class)),
                        new ProjectDeletionController(mock(ProjectDeletionService.class)))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listUsesTheUserFromTheToken() throws Exception {
        when(projectService.list(userId, "draft", null)).thenReturn(List.<ProjectSummaryResponse>of());

        mvc.perform(get("/api/projects").param("status", "draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void invalidProjectAnswers400WithEachField() throws Exception {
        String body = """
                {"name": "ab", "builtAreaM2": 120, "floors": 3,
                 "plannedStartDate": "2026-11-02", "plannedEndDate": "2026-11-01"}
                """;

        mvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.name").exists())
                .andExpect(jsonPath("$.error.fields.projectTypeId").exists())
                .andExpect(jsonPath("$.error.fields.floors").exists())
                .andExpect(jsonPath("$.error.fields.plannedDatesInOrder").exists());
    }

    @Test
    void notAllowedStatusChangeAnswers409() throws Exception {
        UUID projectId = UUID.randomUUID();
        when(projectService.changeStatus(eq(projectId), eq(userId), any()))
                .thenThrow(new ConflictException("INVALID_STATUS_CHANGE", "Not allowed"));

        mvc.perform(patch("/api/projects/{id}/status", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"completed\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_CHANGE"));
    }

    @Test
    void confirmationWithoutTokenAnswers400() throws Exception {
        mvc.perform(post("/api/projects/deletion/confirm").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.token").exists());
    }
}
