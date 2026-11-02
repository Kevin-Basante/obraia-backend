package com.obraia.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Account deletion settings (prefix "app.accounts").
 *
 * @param deletionLinkTtl validity of the link that confirms the deletion
 * @param purgeAfter      time a deleted account is kept before it is removed for good
 */
@ConfigurationProperties(prefix = "app.accounts")
public record AccountProperties(Duration deletionLinkTtl, Duration purgeAfter) {
}
