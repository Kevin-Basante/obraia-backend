package com.obraia.backend.entity.blueprint;

/**
 * Where the file of a blueprint is kept. LOCAL is a folder on the server
 * (development); EXTERNAL is any place outside the server's disk, which in
 * this version is the database (see V4).
 */
public enum StorageProvider {

    LOCAL("local"),
    EXTERNAL("external");

    private final String code;

    StorageProvider(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static StorageProvider fromCode(String code) {
        for (StorageProvider provider : values()) {
            if (provider.code.equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown storage provider: " + code);
    }
}
