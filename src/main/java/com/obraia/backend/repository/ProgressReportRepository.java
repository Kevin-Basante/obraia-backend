package com.obraia.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.phase.ProgressReport;

public interface ProgressReportRepository extends JpaRepository<ProgressReport, UUID> {

    /** Reports of the whole project, newest first. */
    List<ProgressReport> findByTaskPhaseProjectIdOrderByReportDateDescCreatedAtDesc(UUID projectId);

    @EntityGraph(attributePaths = {"reportedBy", "siteManager"})
    List<ProgressReport> findByTaskIdOrderByReportDateDescCreatedAtDesc(UUID taskId);

    boolean existsByTaskPhaseProjectId(UUID projectId);
}
