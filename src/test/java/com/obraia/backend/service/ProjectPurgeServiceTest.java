package com.obraia.backend.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ProjectProperties;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.storage.BlueprintStorage;

class ProjectPurgeServiceTest {

    @Test
    void removesProjectsDeletedBeforeTheKeepPeriodWithTheirFiles() {
        Instant now = Instant.parse("2026-11-02T09:00:00Z");
        ProjectRepository projects = mock(ProjectRepository.class);
        DeletionRequestRepository requests = mock(DeletionRequestRepository.class);
        BlueprintRepository blueprints = mock(BlueprintRepository.class);
        BlueprintStorage storage = mock(BlueprintStorage.class);
        when(blueprints.findFilePathsOfProjectsDeletedBefore(Instant.parse("2026-10-03T09:00:00Z")))
                .thenReturn(List.of("projects/a/plan.pdf"));
        ProjectPurgeService service = new ProjectPurgeService(projects, requests, blueprints, storage,
                new ProjectProperties(Duration.ofMinutes(30), Duration.ofDays(30)), Clock.fixed(now, ClockConfig.ZONE));

        service.purge();

        verify(projects).purgeDeletedBefore(Instant.parse("2026-10-03T09:00:00Z"));
        verify(requests).deleteExpired(now);
        verify(storage).delete("projects/a/plan.pdf");
    }
}
