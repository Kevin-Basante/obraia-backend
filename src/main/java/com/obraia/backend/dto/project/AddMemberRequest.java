package com.obraia.backend.dto.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Email of a registered user who will also manage the project. */
public record AddMemberRequest(@NotBlank @Email String email) {
}
