package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.project.MemberResponse;
import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;

import jakarta.persistence.EntityManager;

/**
 * People who manage a project together. Any member can add another
 * registered user by email; the creator always stays.
 */
@Service
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProjectAccessService accessService;
    private final EntityManager entityManager;

    public ProjectMemberService(ProjectMemberRepository memberRepository, UserRepository userRepository,
            RoleRepository roleRepository, ProjectAccessService accessService, EntityManager entityManager) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accessService = accessService;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        return memberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /** Only verified, active accounts can be added, so nobody gets a project they cannot open. */
    @Transactional
    public MemberResponse add(UUID projectId, UUID userId, String email) {
        accessService.requireMembership(projectId, userId);
        Project project = accessService.requireProject(projectId, userId);
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim())
                .filter(found -> found.isEmailVerified() && found.canSignIn())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "No active ObraIA account uses that email"));
        if (memberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ConflictException("ALREADY_MEMBER", "That user is already a member of the project");
        }
        Role owner = roleRepository.findByName(Role.OWNER)
                .orElseThrow(() -> new IllegalStateException("The seed migration did not create the owner role"));
        ProjectMember member = memberRepository.saveAndFlush(new ProjectMember(project, user, owner));
        // Reads joined_at, which the database fills in.
        entityManager.refresh(member);
        return MemberResponse.from(member);
    }

    @Transactional
    public void remove(UUID projectId, UUID userId, UUID memberUserId) {
        accessService.requireMembership(projectId, userId);
        Project project = accessService.requireProject(projectId, userId);
        if (project.getCreatedBy().equals(memberUserId)) {
            throw new ConflictException("CANNOT_REMOVE_CREATOR", "The creator of the project cannot be removed");
        }
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND",
                        "That user is not a member of the project"));
        memberRepository.delete(member);
    }
}
