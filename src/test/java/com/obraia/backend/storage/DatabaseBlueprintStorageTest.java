package com.obraia.backend.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.obraia.backend.entity.blueprint.StorageProvider;

class DatabaseBlueprintStorageTest {

    private JdbcTemplate jdbc;
    private DatabaseBlueprintStorage storage;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        storage = new DatabaseBlueprintStorage(jdbc);
    }

    @Test
    void isTheExternalProvider() {
        assertEquals(StorageProvider.EXTERNAL, storage.provider());
    }

    @Test
    void savesAndDeletesByKey() {
        byte[] content = {1, 2, 3};

        storage.save("projects/a/plan.pdf", content);
        storage.delete("projects/a/plan.pdf");

        verify(jdbc).update("INSERT INTO blueprint_files (storage_key, content) VALUES (?, ?)", "projects/a/plan.pdf",
                content);
        verify(jdbc).update("DELETE FROM blueprint_files WHERE storage_key = ?", "projects/a/plan.pdf");
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadsTheStoredBytes() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("projects/a/plan.pdf")))
                .thenReturn(List.of(new byte[] {7, 8}));

        assertArrayEquals(new byte[] {7, 8}, storage.load("projects/a/plan.pdf"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aMissingFileIsAnError() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("missing"))).thenReturn(List.of());

        assertThrows(StoredFileNotFoundException.class, () -> storage.load("missing"));
    }
}
