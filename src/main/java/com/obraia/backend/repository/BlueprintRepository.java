package com.obraia.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.blueprint.Blueprint;

public interface BlueprintRepository extends JpaRepository<Blueprint, UUID> {

    List<Blueprint> findByProjectIdOrderByUploadedAtDesc(UUID projectId);

    Optional<Blueprint> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);

    /** Storage keys of the blueprints of the projects the purge is about to remove. */
    @Query("SELECT b.filePath FROM Blueprint b WHERE b.project.deletedAt < :limit")
    List<String> findFilePathsOfProjectsDeletedBefore(@Param("limit") Instant limit);
}
