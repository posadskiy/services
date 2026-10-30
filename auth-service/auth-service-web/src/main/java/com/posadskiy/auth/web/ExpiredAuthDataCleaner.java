package com.posadskiy.auth.web;

import com.posadskiy.auth.core.service.MailOutbox;
import com.posadskiy.auth.core.storage.db.MailOutboxRepository;
import com.posadskiy.auth.core.storage.db.OAuthLoginCodeRepository;
import com.posadskiy.auth.core.storage.db.OAuthStateRepository;
import com.posadskiy.auth.core.storage.db.WebAuthnChallengeRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.time.Instant;

/**
 * Removes data that has outlived its purpose. Auth-service runs as one replica, so a single scheduler is enough.
 * Queued mail is dropped after a day.
 */
@Singleton
public class ExpiredAuthDataCleaner {

    private final OAuthStateRepository stateRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;
    private final WebAuthnChallengeRepository challengeRepository;
    private final MailOutboxRepository mailOutboxRepository;

    public ExpiredAuthDataCleaner(
        OAuthStateRepository stateRepository,
        OAuthLoginCodeRepository loginCodeRepository,
        WebAuthnChallengeRepository challengeRepository,
        MailOutboxRepository mailOutboxRepository) {
        this.stateRepository = stateRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.challengeRepository = challengeRepository;
        this.mailOutboxRepository = mailOutboxRepository;
    }

    @Scheduled(fixedDelay = "5m")
    public void cleanup() {
        Instant now = Instant.now();
        stateRepository.deleteExpired(now);
        loginCodeRepository.deleteExpired(now);
        challengeRepository.deleteExpired(now);
        mailOutboxRepository.deleteCreatedBefore(now.minus(MailOutbox.MAX_AGE));
    }
}
