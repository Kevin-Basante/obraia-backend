package com.obraia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * General information shown at the top of the Swagger page (/api/docs).
 * The "Authorize" button accepts the access token returned by the login.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI obraiaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ObraIA API")
                        .version("0.2.0")
                        .description("REST API for planning, budgeting and tracking small construction projects."))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
