package com.obraia.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obraia.backend.dto.ErrorResponse;
import com.obraia.backend.dto.auth.EmailRequest;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.auth.RegisterRequest;
import com.obraia.backend.dto.auth.TokenRequest;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Public routes for creating an account and managing the session. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, sign in and password recovery")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an account and send the verification email")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Invalid fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "The email is already registered",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return authService.register(request, ClientInfo.from(http));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirm the email with the token from the link")
    @ApiResponse(responseCode = "200", description = "Email verified")
    @ApiResponse(responseCode = "400", description = "Invalid, used or expired link",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request, HttpServletRequest http) {
        return authService.verifyEmail(request.token(), ClientInfo.from(http));
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a new verification link")
    @ApiResponse(responseCode = "202", description = "Same answer whether or not the email exists")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        return authService.resendVerification(request.email());
    }

}
