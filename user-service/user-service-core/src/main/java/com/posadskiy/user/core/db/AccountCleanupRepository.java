package com.posadskiy.user.core.db;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.GenericRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface AccountCleanupRepository
    extends GenericRepository<com.posadskiy.user.core.db.entity.UserEntity, Long> {

    @Query("UPDATE refresh_token SET revoked = TRUE WHERE user_id = :userId")
    void revokeTokens(Long userId);

    @Query("DELETE FROM passkey_credential WHERE user_id = :userId")
    void deletePasskeys(Long userId);

    @Query("DELETE FROM product_preference WHERE user_id = :userId")
    void deletePreferences(Long userId);

    @Query("""
        INSERT INTO email_suppression (email_normalized)
        VALUES (:email)
        ON CONFLICT (email_normalized) DO NOTHING
        """)
    void suppress(String email);
}
