package com.obraia.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;

/**
 * The single place that decides whether a user can use a project: only
 * its members can. For anyone else the project answers 404, so nobody can
 * find out which project ids exist.
 */
@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectAccessService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Project requireProject(UUID projectId, UUID userId) {
        return projectRepository.findVisibleTo(projectId, userId).orElseThrow(() -> notFound(projectId));
    }

    /** The user's membership, with their role in the project. */
    @Transactional(readOnly = true)
    public ProjectMember requireMembership(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> !member.getProject().isDeleted())
                .orElseThrow(() -> notFound(projectId));
    }

    static ResourceNotFoundException notFound(UUID projectId) {
        return new ResourceNotFoundException("PROJECT_NOT_FOUND", "The project " + projectId + " does not exist");
    }
}
