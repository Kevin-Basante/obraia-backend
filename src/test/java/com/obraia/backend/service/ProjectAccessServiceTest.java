package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

/** Only members can use a project; for anyone else it does not exist (404). */
class ProjectAccessServiceTest {

    private final User user = TestUsers.verifiedUser("hash");
    private final Project project = ProjectFixtures.project(user.getId(), "Casa Prueba");

    private ProjectRepository projects;
    private ProjectMemberRepository members;
    private ProjectAccessService access;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        members = mock(ProjectMemberRepository.class);
        access = new ProjectAccessService(projects, members);
    }

    @Test
    void aMemberGetsTheProject() {
        when(projects.findVisibleTo(project.getId(), user.getId())).thenReturn(Optional.of(project));

        assertSame(project, access.requireProject(project.getId(), user.getId()));
    }

    @Test
    void anyoneElseGets404() {
        UUID stranger = UUID.randomUUID();
        when(projects.findVisibleTo(project.getId(), stranger)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> access.requireProject(project.getId(), stranger));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
    }

    @Test
    void theMembershipCarriesTheRole() {
        ProjectMember member = ProjectFixtures.member(project, user);
        when(members.findByProjectIdAndUserId(project.getId(), user.getId())).thenReturn(Optional.of(member));

        assertEquals("owner", access.requireMembership(project.getId(), user.getId()).getRole().getName());
    }

    @Test
    void aDeletedProjectHasNoMembers() {
        ProjectMember member = ProjectFixtures.member(project, user);
        project.markDeleted(Instant.now());
        when(members.findByProjectIdAndUserId(project.getId(), user.getId())).thenReturn(Optional.of(member));

        assertThrows(ResourceNotFoundException.class, () -> access.requireMembership(project.getId(), user.getId()));
    }
}
