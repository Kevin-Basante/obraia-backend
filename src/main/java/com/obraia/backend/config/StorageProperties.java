package com.obraia.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Blueprint storage settings (prefix "app.storage").
 *
 * @param provider          "database" (default, used on Render) or "local"
 * @param localDir          folder used when the provider is "local"
 * @param maxFileSize       largest file accepted
 * @param maxFilesPerProject blueprints a project can have
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String provider, String localDir, DataSize maxFileSize, int maxFilesPerProject) {

    public boolean usesLocalFolder() {
        return "local".equalsIgnoreCase(provider);
    }
}
