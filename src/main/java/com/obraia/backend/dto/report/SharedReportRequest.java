package com.obraia.backend.dto.report;

import jakarta.validation.constraints.NotBlank;

/**
 * Download of a public report with a shared link.
 *
 * @param lang "es" or "en"; English when it is missing
 */
public record SharedReportRequest(@NotBlank String token, String lang) {
}
