package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ProjectProperties;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.project.DeletionImpactResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.DeletionRequest;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ForbiddenException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectContentRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.TokenHasher;

/**
 * Deleting a project takes two steps: the creator asks for it and then
 * confirms it with the link sent by email. The project is hidden right
 * away and removed for good later (see ProjectPurgeService).
 */
@Service
public class ProjectDeletionService {

    private final ProjectRepository projectRepository;
    private final ProjectContentRepository contentRepository;
    private final DeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final ProjectEmailService emailService;
    private final ProjectProperties properties;
    private final Clock clock;

    public ProjectDeletionService(ProjectRepository projectRepository, ProjectContentRepository contentRepository,
            DeletionRequestRepository deletionRequestRepository, UserRepository userRepository,
            ProjectAccessService accessService, ProjectEmailService emailService, ProjectProperties properties,
            Clock clock) {
        this.projectRepository = projectRepository;
        this.contentRepository = contentRepository;
        this.deletionRequestRepository = deletionRequestRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    /** What would be lost, for the warnings shown before asking for the deletion. */
    @Transactional(readOnly = true)
    public DeletionImpactResponse impact(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        LocalDate purgeDate = LocalDate.now(clock).plusDays(properties.purgeAfter().toDays());
        return new DeletionImpactResponse(project.getName(), contentRepository.countContent(projectId), purgeDate,
                properties.deletionLinkTtl().toMinutes());
    }

    /** Sends the confirmation link. Only the creator can delete a project. */
    @Transactional
    public MessageResponse requestDeletion(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        if (!project.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("NOT_PROJECT_CREATOR", "Only the creator of the project can delete it");
        }
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "The user does not exist"));

        deletionRequestRepository.deletePending(DeletionRequest.TARGET_PROJECT, projectId);
        String token = TokenHasher.generateToken();
        deletionRequestRepository.save(new DeletionRequest(userId, DeletionRequest.TARGET_PROJECT, projectId,
                TokenHasher.hash(token), Instant.now(clock).plus(properties.deletionLinkTtl())));
        // If the email fails, the exception rolls back the request too.
        emailService.sendDeletionConfirmation(user, project.getName(), token, properties.deletionLinkTtl(),
                properties.purgeAfter());
        return new MessageResponse("We sent you an email to confirm the deletion of " + project.getName());
    }

    /** Public route: the token from the email is enough, like the other email links. */
    @Transactional
    public MessageResponse confirmDeletion(String token) {
        Instant now = Instant.now(clock);
        DeletionRequest request = deletionRequestRepository.findByTokenHash(TokenHasher.hash(token))
                .filter(found -> DeletionRequest.TARGET_PROJECT.equals(found.getTargetType()))
                .filter(found -> found.isUsable(now))
                .orElseThrow(InvalidTokenException::deletionLink);
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getTargetId())
                .orElseThrow(InvalidTokenException::deletionLink);

        request.markConfirmed(now);
        project.markDeleted(now);
        LocalDate purgeDate = LocalDate.ofInstant(now.plus(properties.purgeAfter()), ClockConfig.ZONE);
        return new MessageResponse("The project " + project.getName()
                + " was deleted. Its data will be removed for good on " + purgeDate + ".");
    }
}
