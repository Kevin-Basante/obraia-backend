package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.dto.project.ProjectRequest;
import com.obraia.backend.dto.project.ProjectResponse;
import com.obraia.backend.dto.project.ProjectSummaryResponse;
import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.catalog.Location;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.entity.project.ProjectStatus;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.repository.LocationRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.ProjectTypeRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.support.CatalogFixtures;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

import jakarta.persistence.EntityManager;

class ProjectServiceTest {

    // 10:00 a.m. in Bogota on 2026-11-02.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T15:00:00Z"), ClockConfig.ZONE);

    private final User user = TestUsers.verifiedUser("hash");
    private final ProjectType type = CatalogFixtures.projectType("Vivienda unifamiliar");
    private final Location city = CatalogFixtures.location("Cali", "Valle del Cauca");

    private ProjectRepository projects;
    private ProjectMemberRepository members;
    private ProjectTypeRepository types;
    private LocationRepository locations;
    private ProjectAccessService access;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        members = mock(ProjectMemberRepository.class);
        types = mock(ProjectTypeRepository.class);
        locations = mock(LocationRepository.class);
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        access = mock(ProjectAccessService.class);
        when(users.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(roles.findByName(Role.OWNER)).thenReturn(Optional.of(ProjectFixtures.ownerRole()));
        when(types.findById(type.getId())).thenReturn(Optional.of(type));
        when(locations.findById(city.getId())).thenReturn(Optional.of(city));
        service = new ProjectService(projects, members, types, locations, users, roles, access,
                mock(EntityManager.class), CLOCK);
    }

    @Test
    void creatorBecomesTheFirstOwner() {
        ProjectResponse response = service.create(user.getId(), request(" Casa Prueba ", type.getId()));

        ArgumentCaptor<ProjectMember> member = ArgumentCaptor.forClass(ProjectMember.class);
        verify(members).save(member.capture());
        assertEquals(user, member.getValue().getUser());
        assertEquals("owner", response.myRole());
        assertEquals("Casa Prueba", response.name());
        assertEquals("draft", response.status().code());
    }

    @Test
    void unknownProjectTypeIsABadRequest() {
        UUID unknown = UUID.randomUUID();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.create(user.getId(), request("Casa Prueba", unknown)));

        assertEquals("INVALID_PROJECT_TYPE", ex.getCode());
        verify(projects, never()).saveAndFlush(any());
    }

    @Test
    void listFiltersByStatusAndSearchIgnoringAccents() {
        when(projects.findVisibleTo(user.getId())).thenReturn(List.of(
                ProjectFixtures.project(user.getId(), "Casa Jamundí"),
                ProjectFixtures.project(user.getId(), "Bodega Norte", ProjectStatus.IN_PROGRESS),
                ProjectFixtures.project(user.getId(), "Casa Centro", ProjectStatus.IN_PROGRESS)));

        List<ProjectSummaryResponse> result = service.list(user.getId(), "in_progress", "casa");

        assertEquals(List.of("Casa Centro"), result.stream().map(ProjectSummaryResponse::name).toList());
        assertEquals(1, service.list(user.getId(), null, "jamundi").size());
        assertEquals(3, service.list(user.getId(), " ", null).size());
    }

    @Test
    void closedProjectsCannotBeEdited() {
        Project project = givenProject(ProjectStatus.COMPLETED);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.update(project.getId(), user.getId(), request("Otro nombre", type.getId())));

        assertEquals("PROJECT_CLOSED", ex.getCode());
    }

    @Test
    void startingTheWorkRecordsTodayInBogota() {
        Project project = givenProject(ProjectStatus.PLANNED);

        ProjectResponse response = service.changeStatus(project.getId(), user.getId(), "in_progress");

        assertEquals(LocalDate.of(2026, 11, 2), response.actualStartDate());
        assertEquals(List.of("paused", "completed", "cancelled"),
                response.nextStatuses().stream().map(status -> status.code()).toList());
    }

    @Test
    void notAllowedChangeListsTheValidOptions() {
        Project project = givenProject(ProjectStatus.DRAFT);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.changeStatus(project.getId(), user.getId(), "completed"));

        assertEquals("INVALID_STATUS_CHANGE", ex.getCode());
        assertTrue(ex.getMessage().endsWith("Allowed: planned, cancelled"));
    }

    private Project givenProject(ProjectStatus status) {
        Project project = ProjectFixtures.project(user.getId(), "Casa Prueba", status);
        when(access.requireMembership(project.getId(), user.getId()))
                .thenReturn(ProjectFixtures.member(project, user));
        when(access.requireProject(project.getId(), user.getId())).thenReturn(project);
        return project;
    }

    private ProjectRequest request(String name, UUID typeId) {
        return new ProjectRequest(name, typeId, city.getId(), null, new BigDecimal("120.00"), 2, null, null);
    }
}
