package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.user.AccountDeletionImpactResponse;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.AccountDeletionService;
import com.obraia.backend.service.AccountService;

/** HTTP side of the account security routes, without the database. */
class AccountControllerTest {

    private final UUID userId = UUID.randomUUID();

    private AccountService accountService;
    private AccountDeletionService deletionService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        accountService = mock(AccountService.class);
        deletionService = mock(AccountDeletionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService, deletionService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePasswordAnswers200() throws Exception {
        when(accountService.changePassword(eq(userId), any(), any()))
                .thenReturn(new MessageResponse("Your password was changed. Sign in again with the new password."));

        mvc.perform(post("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Password123\",\"newPassword\":\"NewPassword456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void aWeakNewPasswordIs400() throws Exception {
        mvc.perform(post("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Password123\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.newPassword").exists());
    }

    @Test
    void aWrongCurrentPasswordIs400OnThatField() throws Exception {
        when(accountService.changePassword(eq(userId), any(), any()))
                .thenThrow(new FieldValidationException(Map.of("currentPassword", "is not correct")));

        mvc.perform(post("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Wrong123\",\"newPassword\":\"NewPassword456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.currentPassword").value("is not correct"));
    }

    @Test
    void deletionImpactListsTheProjects() throws Exception {
        when(deletionService.impact(userId)).thenReturn(new AccountDeletionImpactResponse(List.of("Casa Norte"),
                List.of(), 2, LocalDate.of(2026, 12, 2), 30));

        mvc.perform(get("/api/users/me/deletion-impact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownProjects[0]").value("Casa Norte"))
                .andExpect(jsonPath("$.joinedProjects").value(2));
    }

    @Test
    void deletionRequestAnswers202() throws Exception {
        when(deletionService.requestDeletion(eq(userId), any(), any()))
                .thenReturn(new MessageResponse("We sent you an email to confirm the deletion of your account"));

        mvc.perform(post("/api/users/me/deletion-request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Password123\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void deletionRequestWithoutPasswordIs400() throws Exception {
        mvc.perform(post("/api/users/me/deletion-request").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.password").exists());
    }
}
