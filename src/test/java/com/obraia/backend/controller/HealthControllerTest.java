package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.DatabaseStatus;
import com.obraia.backend.dto.HealthResponse;
import com.obraia.backend.dto.HelloResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.HealthService;

/** Render uses /api/health to know if the service works, so its status code matters. */
class HealthControllerTest {

    private HealthService healthService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        healthService = mock(HealthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new HealthController(healthService), new RootController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void healthyDatabaseAnswers200() throws Exception {
        when(healthService.getHealth())
                .thenReturn(new HealthResponse("ok", 60, DatabaseStatus.connected(4), OffsetDateTime.now()));

        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.database.status").value("connected"));
    }

    @Test
    void databaseDownAnswers503() throws Exception {
        when(healthService.getHealth())
                .thenReturn(new HealthResponse("degraded", 60, DatabaseStatus.disconnected(), OffsetDateTime.now()));

        mvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("degraded"));
    }

    @Test
    void helloShowsTheDataReadFromTheDatabase() throws Exception {
        when(healthService.getHello()).thenReturn(new HelloResponse("Hello world", "ObraIA",
                new HelloResponse.Database("connected", 30, List.of("Vivienda unifamiliar"), 7), OffsetDateTime.now()));

        mvc.perform(get("/api/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.database.projectTypes[0]").value("Vivienda unifamiliar"));
    }

    @Test
    void rootPointsToTheDocs() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docs").value("/api/docs"));
    }
}
