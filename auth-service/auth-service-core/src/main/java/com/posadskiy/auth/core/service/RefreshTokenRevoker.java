package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

/**
 * Family revocation must commit even when the refresh request then returns 401.
 * The caller's transaction rolls back on {@code InvalidCredentialsException}.
 */
@Singleton
public class RefreshTokenRevoker {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    public RefreshTokenRevoker(RefreshTokenRepository refreshTokenRepository, AuditService auditService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
    }

    @Transactional(propagation = TransactionDefinition.Propagation.REQUIRES_NEW)
    public void revokeFamily(UUID familyId, Long userId) {
        refreshTokenRepository.revokeFamily(familyId);
        auditService.record(userId, "REFRESH_REUSE_OR_EXPIRED", null);
    }
}
