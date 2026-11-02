package com.obraia.backend.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.obraia.backend.dto.project.MemberResponse;
import com.obraia.backend.exception.ForbiddenException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.ProjectMemberService;

/** HTTP side of the project member routes, with a signed-in user and without the database. */
class ProjectMemberControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private ProjectMemberService memberService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        memberService = mock(ProjectMemberService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProjectMemberController(memberService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listMarksTheCreator() throws Exception {
        when(memberService.list(projectId, userId)).thenReturn(List.of(new MemberResponse(userId, "Ana", "Rojas",
                "ana@example.com", "owner", true, Instant.parse("2026-11-01T10:00:00Z"))));

        mvc.perform(get("/api/projects/{id}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creator").value(true))
                .andExpect(jsonPath("$[0].role").value("owner"));
    }

    @Test
    void addAnswers201() throws Exception {
        UUID partner = UUID.randomUUID();
        when(memberService.add(projectId, userId, "luis@example.com")).thenReturn(new MemberResponse(partner,
                "Luis", "Paz", "luis@example.com", "owner", false, Instant.now()));

        mvc.perform(post("/api/projects/{id}/members", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"luis@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luis@example.com"));
    }

    @Test
    void addWithABadEmailAnswers400() throws Exception {
        mvc.perform(post("/api/projects/{id}/members", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"luis\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.email").exists());
    }

    @Test
    void removeAnswers204() throws Exception {
        UUID partner = UUID.randomUUID();

        mvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, partner))
                .andExpect(status().isNoContent());

        verify(memberService).remove(projectId, userId, partner);
    }

    @Test
    void theCreatorCannotBeRemoved() throws Exception {
        doThrow(new ForbiddenException("CANNOT_REMOVE_CREATOR", "The creator cannot be removed"))
                .when(memberService).remove(projectId, userId, userId);

        mvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CANNOT_REMOVE_CREATOR"));
    }
}
