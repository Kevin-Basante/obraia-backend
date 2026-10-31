package com.obraia.backend.entity.notification;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the type as its code ("delay_risk"), the value allowed by the CHECK constraint. */
@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType type) {
        return type == null ? null : type.getCode();
    }

    @Override
    public NotificationType convertToEntityAttribute(String code) {
        return code == null ? null : NotificationType.fromCode(code);
    }
}
