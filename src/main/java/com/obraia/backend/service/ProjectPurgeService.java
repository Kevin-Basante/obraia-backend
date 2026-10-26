package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.ProjectProperties;
import com.obraia.backend.repository.BlueprintRepository;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.storage.BlueprintStorage;

/** Removes for good the projects deleted long ago and the deletion links nobody used. */
@Service
public class ProjectPurgeService {

    private static final Logger log = LoggerFactory.getLogger(ProjectPurgeService.class);

    private final ProjectRepository projectRepository;
    private final DeletionRequestRepository deletionRequestRepository;
    private final BlueprintRepository blueprintRepository;
    private final BlueprintStorage storage;
    private final ProjectProperties properties;
    private final Clock clock;

    public ProjectPurgeService(ProjectRepository projectRepository,
            DeletionRequestRepository deletionRequestRepository, BlueprintRepository blueprintRepository,
            BlueprintStorage storage, ProjectProperties properties, Clock clock) {
        this.projectRepository = projectRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.blueprintRepository = blueprintRepository;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    /** Every day at 4:00 a.m. Colombia time, after the token cleanup. */
    @Scheduled(cron = "0 0 4 * * *", zone = "America/Bogota")
    @Transactional
    public void purge() {
        Instant now = Instant.now(clock);
        Instant limit = now.minus(properties.purgeAfter());
        // The blueprint rows go with the project (ON DELETE CASCADE), but their files must be removed here.
        blueprintRepository.findFilePathsOfProjectsDeletedBefore(limit).forEach(storage::delete);
        int projects = projectRepository.purgeDeletedBefore(limit);
        int links = deletionRequestRepository.deleteExpired(now);
        log.info("Purge finished: {} deleted projects removed, {} expired deletion links removed", projects, links);
    }
}
