package com.obraia.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.project.ProjectResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.entity.project.SiteManager;
import com.obraia.backend.repository.ProjectRepository;

/** Links a project with one of the user's site managers, or removes the link. */
@Service
public class ProjectSiteManagerService {

    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;
    private final SiteManagerService siteManagerService;

    public ProjectSiteManagerService(ProjectRepository projectRepository, ProjectAccessService accessService,
            SiteManagerService siteManagerService) {
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.siteManagerService = siteManagerService;
    }

    @Transactional
    public ProjectResponse assign(UUID projectId, UUID userId, UUID siteManagerId) {
        return change(projectId, userId, siteManagerService.require(siteManagerId, userId));
    }

    @Transactional
    public ProjectResponse unassign(UUID projectId, UUID userId) {
        return change(projectId, userId, null);
    }

    private ProjectResponse change(UUID projectId, UUID userId, SiteManager siteManager) {
        ProjectMember member = accessService.requireMembership(projectId, userId);
        Project project = accessService.requireProject(projectId, userId);
        ProjectService.requireEditable(project);
        project.assignSiteManager(siteManager);
        projectRepository.save(project);
        return ProjectResponse.from(project, member.getRole().getName());
    }
}
