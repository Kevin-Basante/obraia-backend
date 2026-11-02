package com.obraia.backend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.config.OpenApiConfig;
import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.user.AccountDeletionImpactResponse;
import com.obraia.backend.dto.user.ChangePasswordRequest;
import com.obraia.backend.dto.user.DeleteAccountRequest;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.service.AccountDeletionService;
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
    private final AccountDeletionService deletionService;

    public AccountController(AccountService accountService, AccountDeletionService deletionService) {
        this.accountService = accountService;
        this.deletionService = deletionService;
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

    @GetMapping("/me/deletion-impact")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "What the user would lose by deleting the account")
    @ApiResponse(responseCode = "200", description = "Own projects, shared projects, joined projects and purge date")
    public AccountDeletionImpactResponse deletionImpact(@AuthenticationPrincipal Jwt jwt) {
        return deletionService.impact(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/me/deletion-request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Send the email that confirms the deletion of the account",
            description = "Needs the password. Nothing is deleted until the link in the email is opened.")
    @ApiResponse(responseCode = "202", description = "Email sent")
    @ApiResponse(responseCode = "400", description = "Wrong password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "The email could not be sent",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse requestDeletion(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeleteAccountRequest request, HttpServletRequest http) {
        return deletionService.requestDeletion(UUID.fromString(jwt.getSubject()), request, ClientInfo.from(http));
    }
}
