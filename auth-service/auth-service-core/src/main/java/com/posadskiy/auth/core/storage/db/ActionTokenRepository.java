package com.posadskiy.auth.core.storage.db;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

import com.posadskiy.auth.core.storage.db.entity.ActionTokenEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import java.time.Instant;
import java.util.Optional;

@JdbcRepository(dialect = POSTGRES)
public interface ActionTokenRepository extends CrudRepository<ActionTokenEntity, Long> {

    Optional<ActionTokenEntity> findByTokenHash(String tokenHash);

    @Query("DELETE FROM action_token WHERE expires_at < :now")
    void deleteExpired(Instant now);
}
