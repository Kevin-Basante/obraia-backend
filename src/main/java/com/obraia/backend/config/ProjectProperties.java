package com.obraia.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Project deletion settings (prefix "app.projects").
 *
 * @param deletionLinkTtl validity of the link that confirms a deletion
 * @param purgeAfter      time a deleted project is kept before it is removed for good
 */
@ConfigurationProperties(prefix = "app.projects")
public record ProjectProperties(Duration deletionLinkTtl, Duration purgeAfter) {
}
