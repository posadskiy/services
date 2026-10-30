package com.posadskiy.auth.web;

import com.posadskiy.auth.core.service.MailOutbox;
import com.posadskiy.auth.core.storage.db.*;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Removes data that has outlived its purpose. Auth-service runs as one replica, so a single scheduler is enough.
 * Retention: expired refresh tokens are kept 90 days (they are the device history behind new-sign-in mail),
 * expired action tokens 7 days, audit events 400 days, queued mail one day.
 */
@Singleton
public class ExpiredAuthDataCleaner {

    static final Duration REFRESH_TOKEN_RETENTION = Duration.ofDays(90);
    static final Duration ACTION_TOKEN_RETENTION = Duration.ofDays(7);
    static final Duration AUDIT_RETENTION = Duration.ofDays(400);

    private static final Logger LOG = LoggerFactory.getLogger(ExpiredAuthDataCleaner.class);

    private final OAuthStateRepository stateRepository;
    private final OAuthLoginCodeRepository loginCodeRepository;
    private final WebAuthnChallengeRepository challengeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ActionTokenRepository actionTokenRepository;
    private final AuditEventRepository auditEventRepository;
    private final MailOutboxRepository mailOutboxRepository;

    public ExpiredAuthDataCleaner(
        OAuthStateRepository stateRepository,
        OAuthLoginCodeRepository loginCodeRepository,
        WebAuthnChallengeRepository challengeRepository,
        RefreshTokenRepository refreshTokenRepository,
        ActionTokenRepository actionTokenRepository,
        AuditEventRepository auditEventRepository,
        MailOutboxRepository mailOutboxRepository) {
        this.stateRepository = stateRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.challengeRepository = challengeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.actionTokenRepository = actionTokenRepository;
        this.auditEventRepository = auditEventRepository;
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

    @Scheduled(cron = "0 17 3 * * *")
    public void retention() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteExpiredBefore(now.minus(REFRESH_TOKEN_RETENTION));
        actionTokenRepository.deleteExpiredBefore(now.minus(ACTION_TOKEN_RETENTION));
        auditEventRepository.deleteCreatedBefore(now.minus(AUDIT_RETENTION));
        LOG.info("retention cleanup finished");
    }
}
