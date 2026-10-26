package com.obraia.backend.storage;

import com.obraia.backend.entity.blueprint.StorageProvider;

/**
 * Where the bytes of the blueprints are kept. The services only use this
 * interface, so the place can change (a local folder, the database, or a
 * cloud service later) without touching them.
 */
public interface BlueprintStorage {

    /** Value saved in blueprints.storage_provider for the files of this storage. */
    StorageProvider provider();

    void save(String key, byte[] content);

    /** @throws StoredFileNotFoundException if nothing is saved under the key */
    byte[] load(String key);

    /** Does nothing if the key does not exist. */
    void delete(String key);
}
