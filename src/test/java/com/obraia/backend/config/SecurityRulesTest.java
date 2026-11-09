package com.obraia.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.obraia.backend.security.JsonAccessDeniedHandler;
import com.obraia.backend.security.JsonAuthenticationEntryPoint;

import jakarta.servlet.Filter;

/**
 * Runs the real security filter chain (SecurityConfig, CorsConfig and
 * JwtConfig) in front of small fake routes, to check which routes are
 * public and how a missing or wrong token is answered. No database is used.
 */
@SpringJUnitWebConfig(SecurityRulesTest.TestConfig.class)
@TestPropertySource(properties = {
    "app.frontend-url=https://obraia-frontend.vercel.app,http://localhost:5173",
    "app.jwt.secret=only-for-tests-0123456789-abcdefghij",
    "app.jwt.issuer=obraia-backend",
    "app.jwt.access-token-ttl=15m",
    "app.jwt.refresh-token-ttl=7d"
})
class SecurityRulesTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableConfigurationProperties(JwtProperties.class)
    @Import({SecurityConfig.class, CorsConfig.class, JwtConfig.class, JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class, FakeRoutes.class})
    static class TestConfig {
    }

    /** Same paths as the real controllers; they only answer a word. */
    @RestController
    static class FakeRoutes {

        @GetMapping("/api/health")
        String health() {
            return "ok";
        }

        @PostMapping("/api/auth/login")
        String login() {
            return "tokens";
        }

        @GetMapping("/api/catalogs/materials")
        String materials() {
            return "materials";
        }

        @PostMapping("/api/catalogs/materials")
        String createMaterial() {
            return "created";
        }

        @PostMapping("/api/projects/deletion/confirm")
        String confirmProjectDeletion() {
            return "project deleted";
        }

        @PostMapping("/api/users/deletion/confirm")
        String confirmAccountDeletion() {
            return "account deleted";
        }

        @PostMapping("/api/reports/approve")
        String approveReport() {
            return "approved";
        }

        @PostMapping("/api/reports/shared")
        String sharedReport() {
            return "pdf";
        }

        @GetMapping("/api/projects/{id}/reports/public")
        String publicReport() {
            return "pdf";
        }

        @GetMapping("/api/ai/wake")
        String wake() {
            return "awake";
        }

        @GetMapping("/api/projects")
        String projects() {
            return "projects";
        }

        @GetMapping("/api/users/me")
        String me(@AuthenticationPrincipal Jwt jwt) {
            return jwt.getSubject();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @Test
    void publicRoutesWorkWithoutToken() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/api/catalogs/materials")).andExpect(content().string("materials"));
    }

    @Test
    void emailLinksWorkWithoutToken() throws Exception {
        mvc.perform(post("/api/projects/deletion/confirm")).andExpect(status().isOk());
        mvc.perform(post("/api/users/deletion/confirm")).andExpect(status().isOk());
    }

    @Test
    void reportLinksWorkWithoutToken() throws Exception {
        mvc.perform(post("/api/reports/approve")).andExpect(status().isOk());
        mvc.perform(post("/api/reports/shared")).andExpect(content().string("pdf"));
    }

    @Test
    void reportsOfAProjectAndTheWakeUpNeedAToken() throws Exception {
        mvc.perform(get("/api/projects/" + UUID.randomUUID() + "/reports/public"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ai/wake")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesAnswer401InJson() throws Exception {
        mvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void catalogsAreOnlyPublicForReading() throws Exception {
        mvc.perform(post("/api/catalogs/materials")).andExpect(status().isUnauthorized());
    }

    @Test
    void aValidTokenOpensTheProtectedRoutes() throws Exception {
        String userId = UUID.randomUUID().toString();
        String token = token("obraia-backend", userId, Instant.now().plusSeconds(600));

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(userId));
    }

    @Test
    void anExpiredTokenIsRejected() throws Exception {
        String token = token("obraia-backend", UUID.randomUUID().toString(), Instant.now().minusSeconds(3600));

        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() throws Exception {
        String token = token("someone-else", UUID.randomUUID().toString(), Instant.now().plusSeconds(600));

        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void aMadeUpTokenIsRejected() throws Exception {
        mvc.perform(get("/api/projects").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void theFrontendCanAskBeforeCalling() throws Exception {
        mvc.perform(options("/api/projects")
                .header("Origin", "https://obraia-frontend.vercel.app")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://obraia-frontend.vercel.app"));
    }

    @Test
    void theFrontendCanReadTheNameOfADownloadedFile() throws Exception {
        mvc.perform(post("/api/reports/shared").header("Origin", "https://obraia-frontend.vercel.app"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"));
    }

    @Test
    void otherSitesCannotCallTheApi() throws Exception {
        mvc.perform(options("/api/projects")
                .header("Origin", "https://evil.example.com")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private String token(String issuer, String subject, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of("user"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
