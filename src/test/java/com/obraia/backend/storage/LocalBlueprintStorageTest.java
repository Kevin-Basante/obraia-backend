package com.obraia.backend.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalBlueprintStorageTest {

    @TempDir
    Path folder;

    @Test
    void savedFileCanBeReadAndDeleted() {
        LocalBlueprintStorage storage = new LocalBlueprintStorage(folder);
        byte[] content = {1, 2, 3};

        storage.save("projects/abc/plan.png", content);
        assertArrayEquals(content, storage.load("projects/abc/plan.png"));

        storage.delete("projects/abc/plan.png");
        assertThrows(StoredFileNotFoundException.class, () -> storage.load("projects/abc/plan.png"));
    }

    @Test
    void deletingAMissingFileDoesNothing() {
        assertDoesNotThrow(() -> new LocalBlueprintStorage(folder).delete("projects/abc/none.png"));
    }

    @Test
    void keyCannotLeaveTheFolder() {
        LocalBlueprintStorage storage = new LocalBlueprintStorage(folder.resolve("uploads"));

        assertThrows(IllegalArgumentException.class, () -> storage.save("../outside.png", new byte[] {1}));
    }
}
