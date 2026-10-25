package com.obraia.backend.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ProjectProperties;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectRepository;

class ProjectPurgeServiceTest {

    @Test
    void removesProjectsDeletedBeforeTheKeepPeriod() {
        Instant now = Instant.parse("2026-11-02T09:00:00Z");
        ProjectRepository projects = mock(ProjectRepository.class);
        DeletionRequestRepository requests = mock(DeletionRequestRepository.class);
        ProjectPurgeService service = new ProjectPurgeService(projects, requests,
                new ProjectProperties(Duration.ofMinutes(30), Duration.ofDays(30)), Clock.fixed(now, ClockConfig.ZONE));

        service.purge();

        verify(projects).purgeDeletedBefore(Instant.parse("2026-10-03T09:00:00Z"));
        verify(requests).deleteExpired(now);
    }
}
