package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.ReportProperties;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.report.FullReportAccessResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.report.ReportRequest;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.ReportRequestRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.TokenHasher;

/**
 * Who can download the full report. The creator of the project always can;
 * any other member asks for it, the creator approves it with the email
 * link, and then the member can download it for a while.
 */
@Service
public class ReportRequestService {

    private final ReportRequestRepository requestRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;
    private final ProjectEmailService emailService;
    private final ReportProperties properties;
    private final Clock clock;

    public ReportRequestService(ReportRequestRepository requestRepository, ProjectRepository projectRepository,
            UserRepository userRepository, ProjectAccessService accessService, ProjectEmailService emailService,
            ReportProperties properties, Clock clock) {
        this.requestRepository = requestRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    /** Where the user stands with the full report of a project. */
    @Transactional(readOnly = true)
    public FullReportAccessResponse access(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        return accessOf(project, userId);
    }

    /** Sends the email to the creator. A new request replaces the pending one of the same member. */
    @Transactional
    public MessageResponse request(UUID projectId, UUID userId) {
        Project project = accessService.requireProject(projectId, userId);
        FullReportAccessResponse access = accessOf(project, userId);
        if (access.canDownload()) {
            throw new BadRequestException("REPORT_ALREADY_ALLOWED", "You can already download the full report");
        }
        User requester = activeUser(userId, "USER_NOT_FOUND", "The user does not exist");
        User owner = activeUser(project.getCreatedBy(), "PROJECT_OWNER_NOT_FOUND",
                "The creator of the project no longer has an account");

        requestRepository.deletePending(projectId, userId);
        String token = TokenHasher.generateToken();
        Instant now = clock.instant();
        requestRepository.save(new ReportRequest(projectId, userId, TokenHasher.hash(token),
                now.plus(properties.approvalLinkTtl()), now));
        // If the email fails, the exception rolls back the request too.
        emailService.sendReportApproval(owner, requester, project.getName(), token, properties.approvalLinkTtl(),
                properties.downloadWindow());
        return MessageResponse.of("report_request_sent",
                "We asked " + owner.getFirstName() + " to approve the full report of " + project.getName(),
                Map.of("owner", owner.getFirstName(), "project", project.getName()));
    }

    /** Public route: the token of the email link is the proof, like the other email links. */
    @Transactional
    public MessageResponse approve(String token) {
        Instant now = clock.instant();
        ReportRequest request = requestRepository.findByTokenHash(TokenHasher.hash(token))
                .filter(found -> found.canBeApproved(now))
                .orElseThrow(InvalidTokenException::reportLink);
        Project project = projectRepository.findByIdAndDeletedAtIsNull(request.getProjectId())
                .orElseThrow(InvalidTokenException::reportLink);
        User requester = userRepository.findByIdAndDeletedAtIsNull(request.getRequestedBy())
                .orElseThrow(InvalidTokenException::reportLink);

        request.approve(now, properties.downloadWindow());
        long hours = properties.downloadWindow().toHours();
        return MessageResponse.of("report_request_approved", requester.getFirstName()
                + " can download the full report of " + project.getName() + " for the next " + hours + " hours",
                Map.of("requester", requester.getFirstName(), "project", project.getName(),
                        "hours", String.valueOf(hours)));
    }

    /** True when the user can download the full report now. */
    @Transactional(readOnly = true)
    public boolean canDownloadFull(Project project, UUID userId) {
        return accessOf(project, userId).canDownload();
    }

    private FullReportAccessResponse accessOf(Project project, UUID userId) {
        if (project.getCreatedBy().equals(userId)) {
            return new FullReportAccessResponse(FullReportAccessResponse.OWNER, null, null);
        }
        Instant now = clock.instant();
        return requestRepository.findFirstByProjectIdAndRequestedByOrderByCreatedAtDesc(project.getId(), userId)
                .map(request -> {
                    if (request.allowsDownload(now)) {
                        return new FullReportAccessResponse(FullReportAccessResponse.APPROVED,
                                request.getCreatedAt(), request.getDownloadUntil());
                    }
                    String status = request.canBeApproved(now) ? FullReportAccessResponse.PENDING
                            : FullReportAccessResponse.EXPIRED;
                    return new FullReportAccessResponse(status, request.getCreatedAt(), null);
                })
                .orElse(new FullReportAccessResponse(FullReportAccessResponse.NONE, null, null));
    }

    private User activeUser(UUID userId, String code, String message) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException(code, message));
    }
}
