package com.obraia.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.project.SiteManagerRequest;
import com.obraia.backend.dto.project.SiteManagerResponse;
import com.obraia.backend.entity.project.SiteManager;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.SiteManagerRepository;

/**
 * Each user's list of site managers. A user only sees the contacts they
 * saved; deleting one leaves its projects without a site manager.
 */
@Service
public class SiteManagerService {

    private final SiteManagerRepository siteManagerRepository;

    public SiteManagerService(SiteManagerRepository siteManagerRepository) {
        this.siteManagerRepository = siteManagerRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteManagerResponse> list(UUID userId) {
        return siteManagerRepository.findByCreatedByOrderByFullNameAsc(userId).stream()
                .map(SiteManagerResponse::from)
                .toList();
    }

    @Transactional
    public SiteManagerResponse create(UUID userId, SiteManagerRequest request) {
        SiteManager manager = new SiteManager(userId, request.fullName().trim(), blankToNull(request.documentNumber()),
                blankToNull(request.phone()));
        return SiteManagerResponse.from(siteManagerRepository.save(manager));
    }

    @Transactional
    public SiteManagerResponse update(UUID siteManagerId, UUID userId, SiteManagerRequest request) {
        SiteManager manager = require(siteManagerId, userId);
        manager.update(request.fullName().trim(), blankToNull(request.documentNumber()), blankToNull(request.phone()));
        return SiteManagerResponse.from(manager);
    }

    /** The database sets site_manager_id to null in the projects that used this contact. */
    @Transactional
    public void delete(UUID siteManagerId, UUID userId) {
        siteManagerRepository.delete(require(siteManagerId, userId));
    }

    @Transactional(readOnly = true)
    public SiteManager require(UUID siteManagerId, UUID userId) {
        return siteManagerRepository.findByIdAndCreatedBy(siteManagerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SITE_MANAGER_NOT_FOUND",
                        "The site manager " + siteManagerId + " does not exist"));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
