package com.obraia.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.project.Project;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /** Projects the user is a member of, newest first, without the deleted ones. */
    @EntityGraph(attributePaths = {"projectType", "location"})
    @Query("""
            SELECT p FROM Project p
            WHERE p.deletedAt IS NULL
              AND EXISTS (SELECT 1 FROM ProjectMember m WHERE m.project = p AND m.user.id = :userId)
            ORDER BY p.createdAt DESC
            """)
    List<Project> findVisibleTo(@Param("userId") UUID userId);

    /** One project, only if the user is a member and it was not deleted. */
    @EntityGraph(attributePaths = {"projectType", "location", "siteManager"})
    @Query("""
            SELECT p FROM Project p
            WHERE p.id = :projectId AND p.deletedAt IS NULL
              AND EXISTS (SELECT 1 FROM ProjectMember m WHERE m.project = p AND m.user.id = :userId)
            """)
    Optional<Project> findVisibleTo(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    /** Removes for good the projects deleted before the limit. Their related rows go with them (ON DELETE CASCADE). */
    @Modifying
    @Query("DELETE FROM Project p WHERE p.deletedAt < :limit")
    int purgeDeletedBefore(@Param("limit") Instant limit);
}
