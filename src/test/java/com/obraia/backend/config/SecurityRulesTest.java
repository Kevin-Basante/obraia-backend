package com.obraia.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

        @GetMapping("/api/projects")
        String projects() {
            return "projects";
        }

        @GetMapping("/api/users/me")
        String me() {
            return "me";
        }
    }

    @Autowired
    private WebApplicationContext context;

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
}
