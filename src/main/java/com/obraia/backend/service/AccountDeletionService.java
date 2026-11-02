package com.obraia.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.AccountProperties;
import com.obraia.backend.dto.user.AccountDeletionImpactResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.UserRepository;

/**
 * Deleting an account takes two steps, like a project: the user asks for
 * it with the password and confirms it with the link sent by email. The
 * account is hidden right away and removed for good later (see
 * AccountPurgeService).
 */
@Service
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final DeletionRequestRepository deletionRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final AccountEmailService emailService;
    private final AccountProperties properties;
    private final Clock clock;

    public AccountDeletionService(UserRepository userRepository, ProjectRepository projectRepository,
            ProjectMemberRepository memberRepository, DeletionRequestRepository deletionRequestRepository,
            PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService, AuditService auditService,
            AccountEmailService emailService, AccountProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    /** What would be lost, for the warnings shown before asking for the deletion. */
    @Transactional(readOnly = true)
    public AccountDeletionImpactResponse impact(UUID userId) {
        requireUser(userId);
        LocalDate purgeDate = LocalDate.now(clock).plusDays(properties.purgeAfter().toDays());
        return new AccountDeletionImpactResponse(
                projectRepository.findOwnedBy(userId).stream().map(Project::getName).toList(),
                projectRepository.findSharedOwnedBy(userId).stream().map(Project::getName).toList(),
                projectRepository.countJoinedBy(userId), purgeDate, properties.deletionLinkTtl().toMinutes());
    }

    private User requireUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "The user does not exist"));
    }
}
