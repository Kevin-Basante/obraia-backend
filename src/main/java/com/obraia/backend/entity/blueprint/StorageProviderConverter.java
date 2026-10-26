package com.obraia.backend.entity.blueprint;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the storage provider as its code ("local" or "external"). */
@Converter(autoApply = true)
public class StorageProviderConverter implements AttributeConverter<StorageProvider, String> {

    @Override
    public String convertToDatabaseColumn(StorageProvider provider) {
        return provider == null ? null : provider.getCode();
    }

    @Override
    public StorageProvider convertToEntityAttribute(String code) {
        return code == null ? null : StorageProvider.fromCode(code);
    }
}
