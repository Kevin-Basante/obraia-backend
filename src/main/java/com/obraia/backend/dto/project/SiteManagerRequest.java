package com.obraia.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contact data of a site manager. The document and the phone are optional. */
public record SiteManagerRequest(
        @NotBlank @Size(min = 3, max = 150) String fullName,
        @Pattern(regexp = "^[0-9A-Za-z.-]{5,20}$", message = "must be a valid document number") String documentNumber,
        @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "must be a valid phone number") String phone) {
}
