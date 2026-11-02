package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.user.ChangePasswordRequest;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Security of the signed-in account. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Account", description = "Password and deletion of the signed-in account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/me/password")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Change the password",
            description = "Needs the current password. Every session is closed, so the user signs in again.")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Wrong current password, weak new password or the same one",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse changePassword(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        return accountService.changePassword(UUID.fromString(jwt.getSubject()), request, ClientInfo.from(http));
    }
}
