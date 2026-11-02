package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.user.ProfileRequest;
import com.obraia.backend.dto.user.UserResponse;
import com.obraia.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Routes of the signed-in user. They require the access token. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Data of the signed-in user")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Profile of the signed-in user")
    @ApiResponse(responseCode = "200", description = "User data")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // The "sub" claim of the token holds the user id.
        return userService.getProfile(UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/me")
    @Operation(summary = "Change the name and the phone of the signed-in user",
            description = "The email cannot change. An empty phone removes it.")
    @ApiResponse(responseCode = "200", description = "Updated user data")
    @ApiResponse(responseCode = "400", description = "Invalid fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest request) {
        return userService.updateProfile(UUID.fromString(jwt.getSubject()), request);
    }
}
