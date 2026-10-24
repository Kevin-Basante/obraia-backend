package com.obraia.backend.support;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.User;

/** Builds users for unit tests. JPA sets the id when saving, so here it is set by reflection. */
public final class TestUsers {

    private TestUsers() {
    }

    public static User verifiedUser(String passwordHash) {
        User user = withId(new User("ana@example.com", passwordHash, "Ana", "Perez", null));
        user.markEmailVerified(Instant.now());
        return user;
    }

    public static User unverifiedUser(String passwordHash) {
        return withId(new User("ana@example.com", passwordHash, "Ana", "Perez", null));
    }

    public static User withId(User user) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, UUID.randomUUID());
            return user;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
