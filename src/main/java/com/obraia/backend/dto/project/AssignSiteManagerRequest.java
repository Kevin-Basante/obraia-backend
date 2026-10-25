package com.obraia.backend.dto.project;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignSiteManagerRequest(@NotNull UUID siteManagerId) {
}
