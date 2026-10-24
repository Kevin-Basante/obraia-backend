package com.obraia.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the periodic tasks marked with @Scheduled (for example, token cleanup). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
