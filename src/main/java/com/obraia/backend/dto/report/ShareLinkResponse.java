package com.obraia.backend.dto.report;

import java.time.Instant;

/** A link that opens the public report without an account. */
public record ShareLinkResponse(String url, Instant expiresAt) {
}
