package com.obraia.backend.service;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.AccountProperties;
import com.obraia.backend.repository.UserRepository;

/** Removes for good the accounts deleted long ago. Their tokens and memberships go with them (ON DELETE CASCADE). */
@Service
public class AccountPurgeService {

    private static final Logger log = LoggerFactory.getLogger(AccountPurgeService.class);

    private final UserRepository userRepository;
    private final AccountProperties properties;
    private final Clock clock;

    public AccountPurgeService(UserRepository userRepository, AccountProperties properties, Clock clock) {
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** Every day at 4:15 a.m. Colombia time, after the projects of those accounts were purged. */
    @Scheduled(cron = "0 15 4 * * *", zone = "America/Bogota")
    @Transactional
    public void purge() {
        Instant limit = Instant.now(clock).minus(properties.purgeAfter());
        int accounts = userRepository.purgeDeletedBefore(limit);
        log.info("Purge finished: {} deleted accounts removed", accounts);
    }
}
