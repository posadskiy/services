package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.OAuthStateEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface OAuthStateRepository extends CrudRepository<OAuthStateEntity, String> {

    @Query("DELETE FROM oauth_state WHERE expires_at < :now")
    void deleteExpired(Instant now);
}
