package com.obraia.backend.dto.project;

import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.project.ProjectMember;

/** `creator` marks the user who created the project; that member cannot be removed. */
public record MemberResponse(UUID userId, String firstName, String lastName, String email, String role,
        boolean creator, Instant joinedAt) {

    public static MemberResponse from(ProjectMember member) {
        return new MemberResponse(member.getUser().getId(), member.getUser().getFirstName(),
                member.getUser().getLastName(), member.getUser().getEmail(), member.getRole().getName(),
                member.getUser().getId().equals(member.getProject().getCreatedBy()), member.getJoinedAt());
    }
}
