package com.posadskiy.auth.web;

import com.posadskiy.auth.core.storage.db.OAuthLoginCodeRepository;
import com.posadskiy.auth.core.storage.db.OAuthStateRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.time.Instant;

@Singleton
public class ExpiredAuthDataCleaner {

    private final OAuthStateRepository stateRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;

    public ExpiredAuthDataCleaner(OAuthStateRepository stateRepository, OAuthLoginCodeRepository loginCodeRepository) {
        this.stateRepository = stateRepository;
        this.loginCodeRepository = loginCodeRepository;
    }

    @Scheduled(fixedDelay = "5m")
    public void cleanup() {
        Instant now = Instant.now();
        stateRepository.deleteExpired(now);
        loginCodeRepository.deleteExpired(now);
    }
}
