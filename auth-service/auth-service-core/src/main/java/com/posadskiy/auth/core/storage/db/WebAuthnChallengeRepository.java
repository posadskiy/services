package com.posadskiy.auth.core.storage.db;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

import com.posadskiy.auth.core.storage.db.entity.WebAuthnChallengeEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import java.time.Instant;

@JdbcRepository(dialect = POSTGRES)
public interface WebAuthnChallengeRepository extends CrudRepository<WebAuthnChallengeEntity, Long> {

    @Query("DELETE FROM webauthn_challenge WHERE expires_at < :now")
    void deleteExpired(Instant now);

    void deleteByUserId(Long userId);
}
