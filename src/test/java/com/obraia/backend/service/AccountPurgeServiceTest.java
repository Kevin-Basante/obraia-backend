package com.obraia.backend.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.obraia.backend.config.AccountProperties;
import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.repository.UserRepository;

class AccountPurgeServiceTest {

    @Test
    void removesAccountsDeletedBeforeTheKeepPeriod() {
        UserRepository users = mock(UserRepository.class);
        AccountPurgeService service = new AccountPurgeService(users,
                new AccountProperties(Duration.ofMinutes(30), Duration.ofDays(30)),
                Clock.fixed(Instant.parse("2026-11-02T09:15:00Z"), ClockConfig.ZONE));

        service.purge();

        verify(users).purgeDeletedBefore(Instant.parse("2026-10-03T09:15:00Z"));
    }
}
