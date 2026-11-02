package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.auth.AuthResponse;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.user.UserResponse;
import com.obraia.backend.exception.EmailAlreadyRegisteredException;
import com.obraia.backend.exception.EmailNotVerifiedException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.exception.InvalidCredentialsException;
import com.obraia.backend.service.AuthService;

/** HTTP side of the public authentication routes, without the database. */
class AuthControllerTest {

    private static final String REGISTER = """
            {"firstName":"Ana","lastName":"Rojas","email":"ana@example.com","password":"Password123"}
            """;
    private static final String LOGIN = """
            {"email":"ana@example.com","password":"Password123"}
            """;

    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerAnswers201() throws Exception {
        when(authService.register(any(), any())).thenReturn(new MessageResponse("Check your email"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Check your email"));
    }

    @Test
    void registerWithBadFieldsAnswers400WithEachField() throws Exception {
        String body = """
                {"firstName":"","lastName":"Rojas","email":"not-an-email","password":"short"}
                """;

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.firstName").exists())
                .andExpect(jsonPath("$.error.fields.email").exists())
                .andExpect(jsonPath("$.error.fields.password").exists());
    }

    @Test
    void registeredEmailAnswers409() throws Exception {
        when(authService.register(any(), any())).thenThrow(new EmailAlreadyRegisteredException());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTheBearerTokens() throws Exception {
        UserResponse user = new UserResponse(UUID.randomUUID(), "ana@example.com", "Ana", "Rojas", null,
                List.of("user"), true);
        when(authService.login(any(), any())).thenReturn(AuthResponse.bearer("access", 900, "refresh", user));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value("ana@example.com"));
    }

    @Test
    void wrongPasswordAnswers401() throws Exception {
        when(authService.login(any(), any())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unverifiedEmailAnswers403() throws Exception {
        when(authService.login(any(), any())).thenThrow(new EmailNotVerifiedException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void logoutAnswers204AndRevokesTheToken() throws Exception {
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"abc\"}"))
                .andExpect(status().isNoContent());

        verify(authService).logout(eq("abc"), any());
    }

    @Test
    void forgotPasswordAnswers202() throws Exception {
        when(authService.forgotPassword(eq("ana@example.com"), any()))
                .thenReturn(new MessageResponse("If the email exists, we sent a link"));

        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\"}"))
                .andExpect(status().isAccepted());
    }
}
