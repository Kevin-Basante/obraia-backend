package com.obraia.backend.dto.project;

import java.util.UUID;

import com.obraia.backend.entity.project.SiteManager;

public record SiteManagerResponse(UUID id, String fullName, String documentNumber, String phone) {

    public static SiteManagerResponse from(SiteManager manager) {
        return new SiteManagerResponse(manager.getId(), manager.getFullName(), manager.getDocumentNumber(),
                manager.getPhone());
    }
}
