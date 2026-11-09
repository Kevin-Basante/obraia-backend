package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.ai.AiServiceClient;
import com.obraia.backend.ai.AiServiceException;
import com.obraia.backend.ai.SummaryRequest;
import com.obraia.backend.config.ReportProperties;
import com.obraia.backend.report.ReportLanguage;
import com.obraia.backend.report.ReportSummary;
import com.obraia.backend.support.ReportFixtures;

class ReportSummaryServiceTest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AiServiceClient ai;
    private ReportSummaryService service;

    @BeforeEach
    void setUp() {
        ai = mock(AiServiceClient.class);
        when(ai.available()).thenReturn(true);
        service = new ReportSummaryService(ai, executor, new ReportProperties(Duration.ofHours(24),
                Duration.ofHours(24), Duration.ofDays(7), Duration.ofMillis(300)));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void asksTheAiInTheLanguageOfTheReport() {
        ReportSummary summary = ReportFixtures.summary();
        when(ai.summarize(any())).thenReturn(summary);

        assertSame(summary, service.summarize(ReportFixtures.fullData(), ReportLanguage.ES));

        ArgumentCaptor<SummaryRequest> request = ArgumentCaptor.forClass(SummaryRequest.class);
        verify(ai).summarize(request.capture());
        assertEquals("es", request.getValue().language());
        assertEquals("full", request.getValue().reportType());
        assertTrue(request.getValue().figures().containsKey("money"));
        assertTrue(request.getValue().figures().containsKey("risk"));
    }

    @Test
    void withoutTheAiThereIsNoSummary() {
        when(ai.available()).thenReturn(false);

        assertNull(service.summarize(ReportFixtures.publicData(), ReportLanguage.EN));
        verify(ai, never()).summarize(any());
    }

    @Test
    void aFailureOfTheAiGivesNoSummary() {
        when(ai.summarize(any())).thenThrow(new AiServiceException("down", null));

        assertNull(service.summarize(ReportFixtures.publicData(), ReportLanguage.EN));
    }

    @Test
    void aSlowAiGivesNoSummary() {
        when(ai.summarize(any())).thenAnswer(call -> {
            Thread.sleep(2000);
            return ReportFixtures.summary();
        });

        long start = System.nanoTime();
        assertNull(service.summarize(ReportFixtures.publicData(), ReportLanguage.EN));
        assertTrue(System.nanoTime() - start < 1_500_000_000L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void thePublicFiguresHaveNoMoneyOrRisk() {
        Map<String, Object> figures = ReportSummaryService.figures(ReportFixtures.publicData());

        assertFalse(figures.containsKey("money"));
        assertFalse(figures.containsKey("risk"));
        assertEquals("2026-11-09", figures.get("today"));
        Map<String, Object> project = (Map<String, Object>) figures.get("project");
        assertEquals("Casa Basante", project.get("name"));
        assertNull(project.get("planned_start"));
        Map<String, Object> work = (Map<String, Object>) figures.get("work");
        assertEquals(1, work.get("late_tasks"));
        assertEquals("in_progress", ((List<Map<String, Object>>) work.get("phases")).get(1).get("status"));
    }
}
