package com.obraia.backend.storage;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.obraia.backend.entity.blueprint.StorageProvider;

/**
 * Keeps the files in the blueprint_files table (V4). Used in production,
 * because Render's free plan erases its disk on every deploy.
 */
public class DatabaseBlueprintStorage implements BlueprintStorage {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseBlueprintStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.EXTERNAL;
    }

    @Override
    public void save(String key, byte[] content) {
        jdbcTemplate.update("INSERT INTO blueprint_files (storage_key, content) VALUES (?, ?)", key, content);
    }

    @Override
    public byte[] load(String key) {
        List<byte[]> rows = jdbcTemplate.query("SELECT content FROM blueprint_files WHERE storage_key = ?",
                (row, index) -> row.getBytes("content"), key);
        if (rows.isEmpty()) {
            throw new StoredFileNotFoundException(key);
        }
        return rows.get(0);
    }

    @Override
    public void delete(String key) {
        jdbcTemplate.update("DELETE FROM blueprint_files WHERE storage_key = ?", key);
    }
}
