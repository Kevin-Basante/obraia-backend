package com.obraia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** General information shown at the top of the Swagger page (/api/docs). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI obraiaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ObraIA API")
                .version("0.1.0")
                .description("REST API for planning, budgeting and tracking small construction projects."));
    }
}
