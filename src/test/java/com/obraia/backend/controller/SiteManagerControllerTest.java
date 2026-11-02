package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.obraia.backend.dto.project.SiteManagerResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.service.SiteManagerService;

/** HTTP side of the site manager routes, with a signed-in user and without the database. */
class SiteManagerControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();

    private SiteManagerService siteManagerService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        siteManagerService = mock(SiteManagerService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SiteManagerController(siteManagerService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listReturnsTheManagersOfTheUser() throws Exception {
        when(siteManagerService.list(userId))
                .thenReturn(List.of(new SiteManagerResponse(managerId, "Pedro Gómez", null, "3001234567")));

        mvc.perform(get("/api/site-managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Pedro Gómez"));
    }

    @Test
    void createAnswers201() throws Exception {
        when(siteManagerService.create(eq(userId), any()))
                .thenReturn(new SiteManagerResponse(managerId, "Pedro Gómez", "1085123456", null));

        mvc.perform(post("/api/site-managers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Pedro Gómez\",\"documentNumber\":\"1085123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(managerId.toString()));
    }

    @Test
    void invalidFieldsAnswer400() throws Exception {
        mvc.perform(post("/api/site-managers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"P\",\"documentNumber\":\"#1\",\"phone\":\"call me\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.fullName").exists())
                .andExpect(jsonPath("$.error.fields.documentNumber").exists())
                .andExpect(jsonPath("$.error.fields.phone").exists());
    }

    @Test
    void updatingAnotherUsersManagerAnswers404() throws Exception {
        when(siteManagerService.update(eq(managerId), eq(userId), any()))
                .thenThrow(new ResourceNotFoundException("SITE_MANAGER_NOT_FOUND", "The site manager does not exist"));

        mvc.perform(put("/api/site-managers/{id}", managerId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Pedro Gómez\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SITE_MANAGER_NOT_FOUND"));
    }

    @Test
    void deleteAnswers204() throws Exception {
        mvc.perform(delete("/api/site-managers/{id}", managerId))
                .andExpect(status().isNoContent());

        verify(siteManagerService).delete(managerId, userId);
    }
}
