package com.obraia.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.ReportProperties;
import com.obraia.backend.dto.report.ShareLinkResponse;
import com.obraia.backend.email.EmailText;
import com.obraia.backend.entity.report.ReportShare;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.ReportShareRepository;
import com.obraia.backend.security.TokenHasher;

/**
 * Links that open the public report of a project without an account. Any
 * member can make one; it only gives the public report, never the full one.
 */
@Service
public class ReportShareService {

    private final ReportShareRepository shareRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService accessService;
    private final ReportProperties properties;
    private final Clock clock;
    private final String frontendBaseUrl;

    public ReportShareService(ReportShareRepository shareRepository, ProjectRepository projectRepository,
            ProjectAccessService accessService, ReportProperties properties, Clock clock,
            @Value("${app.frontend-url}") String frontendUrls) {
        this.shareRepository = shareRepository;
        this.projectRepository = projectRepository;
        this.accessService = accessService;
        this.properties = properties;
        this.clock = clock;
        this.frontendBaseUrl = EmailText.firstUrl(frontendUrls);
    }

    /** A new link; the token is only in the link, the database keeps its hash. */
    @Transactional
    public ShareLinkResponse create(UUID projectId, UUID userId) {
        accessService.requireMembership(projectId, userId);
        String token = TokenHasher.generateToken();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.shareLinkTtl());
        shareRepository.save(new ReportShare(projectId, userId, TokenHasher.hash(token), expiresAt, now));
        String url = frontendBaseUrl + "/shared-report?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        return new ShareLinkResponse(url, expiresAt);
    }

    /** The project of a link that still works, or an error for one that expired or whose project was deleted. */
    @Transactional(readOnly = true)
    public UUID projectOf(String token) {
        Instant now = clock.instant();
        return shareRepository.findByTokenHash(TokenHasher.hash(token))
                .filter(share -> share.isUsable(now))
                .map(ReportShare::getProjectId)
                .filter(projectId -> projectRepository.findByIdAndDeletedAtIsNull(projectId).isPresent())
                .orElseThrow(InvalidTokenException::shareLink);
    }
}
