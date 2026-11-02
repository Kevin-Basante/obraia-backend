package com.obraia.backend.dto.user;

import jakarta.validation.constraints.NotBlank;

/** The password is asked again so a forgotten open session cannot delete the account. */
public record DeleteAccountRequest(@NotBlank String password) {
}
