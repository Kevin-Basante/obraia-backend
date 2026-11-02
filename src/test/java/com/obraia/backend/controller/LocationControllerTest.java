package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.catalog.LocationResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.LocationService;

class LocationControllerTest {

    private LocationService locationService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        locationService = mock(LocationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LocationController(locationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void passesTheFiltersAndLetsTheBrowserCacheTheAnswer() throws Exception {
        when(locationService.listLocations("Nariño", "pasto")).thenReturn(List.of(
                new LocationResponse(UUID.randomUUID(), "Pasto", "Nariño", "Colombia", new BigDecimal("4.50"))));

        mvc.perform(get("/api/catalogs/locations").param("department", "Nariño").param("search", "pasto"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=600, public"))
                .andExpect(jsonPath("$[0].city").value("Pasto"))
                .andExpect(jsonPath("$[0].transportPct").value(4.50));
    }

    @Test
    void listsTheDepartments() throws Exception {
        when(locationService.listDepartments()).thenReturn(List.of("Antioquia", "Nariño"));

        mvc.perform(get("/api/catalogs/locations/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("Nariño"));
    }
}
