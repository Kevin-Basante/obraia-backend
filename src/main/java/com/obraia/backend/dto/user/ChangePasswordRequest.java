package com.obraia.backend.dto.user;

import com.obraia.backend.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

/** The current password proves it is the owner; the new one follows the same rules as at sign-up. */
public record ChangePasswordRequest(@NotBlank String currentPassword, @StrongPassword String newPassword) {
}
