package com.obraia.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import com.obraia.backend.dto.user.UserResponse;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.service.UserService;

/** HTTP side of the account routes, without the database. */
class UserControllerTest {

    private final UUID userId = UUID.randomUUID();

    private UserService userService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        userService = mock(UserService.class);
        mvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserResponse user(String firstName) {
        return new UserResponse(userId, "ana@example.com", firstName, "Perez", null, List.of("owner"), true);
    }

    @Test
    void meReturnsTheProfile() throws Exception {
        when(userService.getProfile(userId)).thenReturn(user("Ana"));

        mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void updateProfileAnswers200() throws Exception {
        when(userService.updateProfile(eq(userId), any())).thenReturn(user("María"));

        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"María\",\"lastName\":\"Perez\",\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("María"));
    }

    @Test
    void blankNameAndBadPhoneAre400() throws Exception {
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\" \",\"lastName\":\"Perez\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.firstName").exists())
                .andExpect(jsonPath("$.error.fields.phone").value("must be a valid phone number"));
    }
}
