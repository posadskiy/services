package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface RefreshTokenRepository extends CrudRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    List<RefreshTokenEntity> findByUserIdAndRevokedFalseAndExpiresAtGreaterThan(Long userId, Instant expiresAt);

    Optional<RefreshTokenEntity> findByFamilyIdAndUserIdAndRevokedFalse(UUID familyId, Long userId);

    @Query("""
        UPDATE refresh_token
        SET revoked = TRUE, last_used_at = :usedAt
        WHERE token_hash = :tokenHash
          AND revoked = FALSE
          AND expires_at > :usedAt
        """)
    long consumeIfCurrent(String tokenHash, Instant usedAt);

    /**
     * The oldest row of a family is the sign-in itself; later rows are rotations. The cleaner only drops rows long
     * past expiry, so a live family always keeps rows older than its last rotation window.
     */
    Optional<RefreshTokenEntity> findTopByFamilyIdOrderByCreatedAtAsc(UUID familyId);

    boolean existsByUserId(Long userId);

    boolean existsByUserIdAndUserAgent(Long userId, String userAgent);

    @Query("DELETE FROM refresh_token WHERE expires_at < :before")
    void deleteExpiredBefore(Instant before);

    @Query("UPDATE refresh_token SET revoked = TRUE WHERE family_id = :familyId")
    void revokeFamily(UUID familyId);

    @Query("UPDATE refresh_token SET revoked = TRUE WHERE family_id = :familyId AND user_id = :userId AND revoked = FALSE")
    int revokeFamilyForUser(UUID familyId, Long userId);

    @Query("UPDATE refresh_token SET revoked = TRUE WHERE user_id = :userId")
    void revokeAllForUser(Long userId);
}
