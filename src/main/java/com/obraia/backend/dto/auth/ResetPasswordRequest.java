package com.obraia.backend.dto.auth;

import com.obraia.backend.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String token, @StrongPassword String newPassword) {
}
