package com.obraia.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Body of "resend verification" and "forgot password". */
public record EmailRequest(@NotBlank @Email String email) {
}
