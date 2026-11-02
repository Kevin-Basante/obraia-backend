package com.obraia.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** New name and phone of the signed-in user. An empty phone removes it. The email cannot change. */
public record ProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Pattern(regexp = "^$|^[+0-9 ()-]{7,20}$", message = "must be a valid phone number") String phone) {
}
