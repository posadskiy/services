package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.ActionTokenEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface ActionTokenRepository extends CrudRepository<ActionTokenEntity, Long> {

    Optional<ActionTokenEntity> findByTokenHash(String tokenHash);

    @Query("""
        UPDATE action_token
        SET consumed_at = :now
        WHERE user_id = :userId AND purpose = :purpose AND consumed_at IS NULL
        """)
    void consumeOpen(Long userId, String purpose, Instant now);

    @Query("""
        UPDATE action_token
        SET consumed_at = :now
        WHERE token_hash = :tokenHash
          AND purpose = :purpose
          AND consumed_at IS NULL
          AND expires_at > :now
        """)
    long consumeIfOpen(String tokenHash, String purpose, Instant now);
}
