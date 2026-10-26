package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface RefreshTokenRepository extends CrudRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("UPDATE refresh_token SET revoked = TRUE WHERE family_id = :familyId")
    void revokeFamily(UUID familyId);

    @Query("UPDATE refresh_token SET revoked = TRUE WHERE user_id = :userId")
    void revokeAllForUser(Long userId);
}
