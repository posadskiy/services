package com.posadskiy.auth.core.storage.db;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

import com.posadskiy.auth.core.storage.db.entity.ExternalIdentityEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = POSTGRES)
public interface ExternalIdentityRepository extends CrudRepository<ExternalIdentityEntity, Long> {

    Optional<ExternalIdentityEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<ExternalIdentityEntity> findByUserId(Long userId);
}

