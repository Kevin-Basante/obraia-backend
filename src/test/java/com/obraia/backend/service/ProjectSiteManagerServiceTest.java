package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.entity.project.SiteManager;
import com.obraia.backend.exception.AppException;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class ProjectSiteManagerServiceTest {

    private final User user = TestUsers.verifiedUser("hash");
    private final SiteManager manager = new SiteManager(user.getId(), "Carlos Pérez", "1061234567", "3001234567");

    private ProjectRepository projects;
    private ProjectAccessService access;
    private SiteManagerService siteManagers;
    private ProjectSiteManagerService service;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        access = mock(ProjectAccessService.class);
        siteManagers = mock(SiteManagerService.class);
        service = new ProjectSiteManagerService(projects, access, siteManagers);
    }

    private Project givenProject(ProjectStatus status) {
        Project project = ProjectFixtures.project(user.getId(), "Casa Prueba", status);
        when(access.requireMembership(project.getId(), user.getId()))
                .thenReturn(ProjectFixtures.member(project, user));
        when(access.requireProject(project.getId(), user.getId())).thenReturn(project);
        return project;
    }

    @Test
    void assignsOneOfTheUsersManagers() {
        Project project = givenProject(ProjectStatus.DRAFT);
        UUID managerId = UUID.randomUUID();
        when(siteManagers.require(managerId, user.getId())).thenReturn(manager);

        service.assign(project.getId(), user.getId(), managerId);

        assertEquals("Carlos Pérez", project.getSiteManager().getFullName());
        verify(projects).save(project);
    }

    @Test
    void unassignRemovesTheManager() {
        Project project = givenProject(ProjectStatus.IN_PROGRESS);
        project.assignSiteManager(manager);

        service.unassign(project.getId(), user.getId());

        assertNull(project.getSiteManager());
    }

    @Test
    void aFinishedProjectCannotChange() {
        Project project = givenProject(ProjectStatus.COMPLETED);

        assertThrows(AppException.class, () -> service.unassign(project.getId(), user.getId()));
        verify(projects, never()).save(any());
    }
}
