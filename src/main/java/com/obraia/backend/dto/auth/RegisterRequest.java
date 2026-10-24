package com.obraia.backend.dto.auth;

import com.obraia.backend.validation.StrongPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Data sent by the registration form. The phone number is optional. */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @StrongPassword String password,
        @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "must be a valid phone number") String phone) {
}
