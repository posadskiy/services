package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.OAuthLoginCodeEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface OAuthLoginCodeRepository extends CrudRepository<OAuthLoginCodeEntity, String> {

    @Query("DELETE FROM oauth_login_code WHERE expires_at < :now")
    void deleteExpired(Instant now);

    @Query("SELECT * FROM oauth_login_code WHERE code_hash = :codeHash FOR UPDATE")
    Optional<OAuthLoginCodeEntity> lockByHash(String codeHash);
}
