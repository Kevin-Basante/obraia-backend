package com.obraia.backend.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.obraia.backend.storage.BlueprintStorage;
import com.obraia.backend.storage.DatabaseBlueprintStorage;
import com.obraia.backend.storage.LocalBlueprintStorage;

/**
 * Chooses where the blueprint files are kept (Bridge pattern: the
 * services work with BlueprintStorage and never know which one it is).
 */
@Configuration
public class StorageConfig {

    @Bean
    public BlueprintStorage blueprintStorage(StorageProperties properties, JdbcTemplate jdbcTemplate) {
        if (properties.usesLocalFolder()) {
            return new LocalBlueprintStorage(Path.of(properties.localDir()));
        }
        return new DatabaseBlueprintStorage(jdbcTemplate);
    }
}
