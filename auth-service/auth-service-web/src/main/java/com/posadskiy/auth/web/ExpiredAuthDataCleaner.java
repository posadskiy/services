package com.posadskiy.auth.web;

import com.posadskiy.auth.core.storage.db.OAuthLoginCodeRepository;
import com.posadskiy.auth.core.storage.db.OAuthStateRepository;
import com.posadskiy.auth.core.storage.db.WebAuthnChallengeRepository;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.time.Instant;

@Singleton
public class ExpiredAuthDataCleaner {

    private final OAuthStateRepository stateRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;
    private final WebAuthnChallengeRepository challengeRepository;

    public ExpiredAuthDataCleaner(
        OAuthStateRepository stateRepository,
        OAuthLoginCodeRepository loginCodeRepository,
        WebAuthnChallengeRepository challengeRepository) {
        this.stateRepository = stateRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.challengeRepository = challengeRepository;
    }

    @Scheduled(fixedDelay = "5m")
    public void cleanup() {
        Instant now = Instant.now();
        stateRepository.deleteExpired(now);
        loginCodeRepository.deleteExpired(now);
        challengeRepository.deleteExpired(now);
    }
}
