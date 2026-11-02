package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.entity.project.ProjectMemberId;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    @EntityGraph(attributePaths = {"user", "role"})
    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(UUID projectId);

    @EntityGraph(attributePaths = "role")
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    /** Takes the user out of the projects created by other people. */
    @Modifying
    @Query("""
            DELETE FROM ProjectMember m
            WHERE m.id.userId = :userId
              AND m.id.projectId IN (SELECT p.id FROM Project p WHERE p.createdBy <> :userId)
            """)
    int deleteFromOthersProjects(@Param("userId") UUID userId);
}
