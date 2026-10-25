package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.LocationRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.ProjectTypeRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.util.TextSearch;

import jakarta.persistence.EntityManager;

/** Create, list, read and edit projects, and move them through their statuses. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectTypeRepository projectTypeRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectAccessService accessService;
    private final EntityManager entityManager;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
            ProjectTypeRepository projectTypeRepository, LocationRepository locationRepository,
            UserRepository userRepository, RoleRepository roleRepository, ProjectAccessService accessService,
            EntityManager entityManager) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.projectTypeRepository = projectTypeRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accessService = accessService;
        this.entityManager = entityManager;
    }

    /** Creates a draft project. The creator becomes its first member, with the owner role. */
    @Transactional
    public ProjectResponse create(UUID userId, ProjectRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "The user does not exist"));
        Role owner = roleRepository.findByName(Role.OWNER)
                .orElseThrow(() -> new IllegalStateException("The seed migration did not create the owner role"));

        Project project = new Project(userId, findProjectType(request), findLocation(request), request.name().trim(),
                trimToNull(request.address()), request.builtAreaM2(), request.floors(), request.plannedStartDate(),
                request.plannedEndDate());
        projectRepository.saveAndFlush(project);
        memberRepository.save(new ProjectMember(project, user, owner));
        // Reads the values the database filled in (created_at, updated_at).
        entityManager.refresh(project);
        return ProjectResponse.from(project, owner.getName());
    }

    /** Projects of the user, newest first. Both filters are optional; the search ignores accents. */
    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> list(UUID userId, String statusCode, String search) {
        ProjectStatus status = statusCode == null || statusCode.isBlank() ? null : ProjectStatus.fromCode(statusCode);
        return projectRepository.findVisibleTo(userId).stream()
                .filter(project -> status == null || project.getStatus() == status)
                .filter(project -> TextSearch.matches(project.getName() + " " + project.getLocation().getCity(),
                        search))
                .map(ProjectSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID userId) {
        ProjectMember member = accessService.requireMembership(projectId, userId);
        return ProjectResponse.from(accessService.requireProject(projectId, userId), member.getRole().getName());
    }

    /** Edits the project data. Finished or cancelled projects are read only. */
    @Transactional
    public ProjectResponse update(UUID projectId, UUID userId, ProjectRequest request) {
        ProjectMember member = accessService.requireMembership(projectId, userId);
        Project project = accessService.requireProject(projectId, userId);
        requireEditable(project);
        project.update(findProjectType(request), findLocation(request), request.name().trim(),
                trimToNull(request.address()), request.builtAreaM2(), request.floors(), request.plannedStartDate(),
                request.plannedEndDate());
        projectRepository.saveAndFlush(project);
        entityManager.refresh(project);
        return ProjectResponse.from(project, member.getRole().getName());
    }

    static void requireEditable(Project project) {
        if (project.getStatus().nextStatuses().isEmpty()) {
            throw new ConflictException("PROJECT_CLOSED",
                    "A " + project.getStatus().getCode() + " project cannot be edited");
        }
    }

    private ProjectType findProjectType(ProjectRequest request) {
        return projectTypeRepository.findById(request.projectTypeId())
                .orElseThrow(() -> new BadRequestException("INVALID_PROJECT_TYPE",
                        "The project type " + request.projectTypeId() + " does not exist"));
    }

    private Location findLocation(ProjectRequest request) {
        return locationRepository.findById(request.locationId())
                .orElseThrow(() -> new BadRequestException("INVALID_LOCATION",
                        "The city " + request.locationId() + " does not exist"));
    }

    private static String trimToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
