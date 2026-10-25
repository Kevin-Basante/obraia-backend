package com.obraia.backend.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock used for dates like "today". Users are in Colombia, so the day
 * changes at midnight Bogota time. Tests replace it with a fixed clock.
 */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
