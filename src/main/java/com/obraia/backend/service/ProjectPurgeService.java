package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.ProjectProperties;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectRepository;

/** Removes for good the projects deleted long ago and the deletion links nobody used. */
@Service
public class ProjectPurgeService {

    private static final Logger log = LoggerFactory.getLogger(ProjectPurgeService.class);

    private final ProjectRepository projectRepository;
    private final DeletionRequestRepository deletionRequestRepository;
    private final ProjectProperties properties;
    private final Clock clock;

    public ProjectPurgeService(ProjectRepository projectRepository,
            DeletionRequestRepository deletionRequestRepository, ProjectProperties properties, Clock clock) {
        this.projectRepository = projectRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Every day at 4:00 a.m. Colombia time, after the token cleanup. */
    @Scheduled(cron = "0 0 4 * * *", zone = "America/Bogota")
    @Transactional
    public void purge() {
        Instant now = Instant.now(clock);
        int projects = projectRepository.purgeDeletedBefore(now.minus(properties.purgeAfter()));
        int links = deletionRequestRepository.deleteExpired(now);
        log.info("Purge finished: {} deleted projects removed, {} expired deletion links removed", projects, links);
    }
}
