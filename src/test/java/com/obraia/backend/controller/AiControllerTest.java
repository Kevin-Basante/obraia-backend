package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.ai.AiWakeResponse;
import com.obraia.backend.service.AiWakeService;

class AiControllerTest {

    @Test
    void wakeAnswersAtOnce() throws Exception {
        AiWakeService wakeService = mock(AiWakeService.class);
        when(wakeService.wake()).thenReturn(new AiWakeResponse(true));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiController(wakeService)).build();

        mvc.perform(get("/api/ai/wake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
