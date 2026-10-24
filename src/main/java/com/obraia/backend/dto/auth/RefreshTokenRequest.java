package com.obraia.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Body of the refresh and logout endpoints. */
public record RefreshTokenRequest(@NotBlank String refreshToken) {
}
