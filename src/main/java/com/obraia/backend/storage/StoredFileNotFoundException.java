package com.obraia.backend.storage;

/** The metadata of a blueprint exists but its file is missing from the storage. */
public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(String key) {
        super("No file is stored under the key " + key);
    }
}
