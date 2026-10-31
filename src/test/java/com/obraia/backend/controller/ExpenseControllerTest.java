package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

import com.obraia.backend.dto.expense.ExpenseRequest;
import com.obraia.backend.dto.expense.ExpenseResponse;
import com.obraia.backend.dto.expense.ExpenseSummaryResponse;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.ExpenseService;

/** HTTP side of the expense routes, without the database. */
class ExpenseControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private ExpenseService expenseService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        expenseService = mock(ExpenseService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ExpenseController(expenseService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAnswers201() throws Exception {
        when(expenseService.create(any(), any(), any())).thenReturn(new ExpenseResponse(UUID.randomUUID(),
                new StatusResponse("materials", "Materials"), "Cemento", new BigDecimal("650000.00"),
                LocalDate.of(2026, 11, 10), null, null, "Ana Test", Instant.parse("2026-11-10T15:00:00Z")));

        mvc.perform(post("/api/projects/{id}/expenses", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"materials\",\"description\":\"Cemento\",\"amount\":650000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.code").value("materials"))
                .andExpect(jsonPath("$.registeredBy").value("Ana Test"));
    }

    @Test
    void zeroAmountAndMissingDescriptionAre400() throws Exception {
        mvc.perform(post("/api/projects/{id}/expenses", projectId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"materials\",\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.amount").exists())
                .andExpect(jsonPath("$.error.fields.description").exists());
        verify(expenseService, never()).create(any(), any(), any(ExpenseRequest.class));
    }

    @Test
    void summaryAnswersTheTotals() throws Exception {
        when(expenseService.summary(projectId, userId)).thenReturn(new ExpenseSummaryResponse(
                new BigDecimal("1650000.00"), new BigDecimal("5000000.00"), new BigDecimal("3350000.00"),
                new BigDecimal("33.00"), List.of()));

        mvc.perform(get("/api/projects/{id}/expenses/summary", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentPct").value(33.0))
                .andExpect(jsonPath("$.remaining").value(3350000.0));
    }

    @Test
    void deleteAnswers204() throws Exception {
        UUID expenseId = UUID.randomUUID();

        mvc.perform(delete("/api/projects/{id}/expenses/{expenseId}", projectId, expenseId))
                .andExpect(status().isNoContent());

        verify(expenseService).delete(projectId, userId, expenseId);
    }
}
