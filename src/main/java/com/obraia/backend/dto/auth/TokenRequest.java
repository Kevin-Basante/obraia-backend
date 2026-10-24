package com.obraia.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/** Token taken from the link that the user received by email. */
public record TokenRequest(@NotBlank String token) {
}
