package com.posadskiy.auth.web;

import com.posadskiy.auth.core.storage.db.ActionTokenRepository;
import com.posadskiy.auth.core.storage.db.OAuthLoginCodeRepository;
import com.posadskiy.auth.core.storage.db.OAuthStateRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import java.time.Instant;

@Singleton
public class ExpiredAuthDataCleaner {

    private final OAuthStateRepository stateRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;
    private final ActionTokenRepository actionTokenRepository;

    public ExpiredAuthDataCleaner(
            OAuthStateRepository stateRepository,
            OAuthLoginCodeRepository loginCodeRepository,
            ActionTokenRepository actionTokenRepository) {
        this.stateRepository = stateRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.actionTokenRepository = actionTokenRepository;
    }

    @Scheduled(fixedDelay = "5m")
    public void cleanup() {
        Instant now = Instant.now();
        stateRepository.deleteExpired(now);
        loginCodeRepository.deleteExpired(now);
        actionTokenRepository.deleteExpired(now);
    }
}
