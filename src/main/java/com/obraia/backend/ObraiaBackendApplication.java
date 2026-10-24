package com.obraia.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Entry point: starts the ObraIA REST API. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ObraiaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObraiaBackendApplication.class, args);
    }
}
