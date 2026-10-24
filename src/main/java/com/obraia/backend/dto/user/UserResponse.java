package com.obraia.backend.dto.user;

import java.util.List;
import java.util.UUID;

import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;

/** Public data of a user. The password hash never leaves the backend. */
public record UserResponse(UUID id, String email, String firstName, String lastName, String phone,
        List<String> roles, boolean emailVerified) {

    public static UserResponse from(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), roles, user.isEmailVerified());
    }
}
