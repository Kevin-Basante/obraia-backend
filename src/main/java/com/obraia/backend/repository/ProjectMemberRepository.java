package com.obraia.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.entity.project.ProjectMemberId;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    @EntityGraph(attributePaths = {"user", "role"})
    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(UUID projectId);

    @EntityGraph(attributePaths = "role")
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
}
