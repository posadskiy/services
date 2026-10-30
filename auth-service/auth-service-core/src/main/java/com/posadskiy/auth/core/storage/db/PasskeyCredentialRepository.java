package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.PasskeyCredentialEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface PasskeyCredentialRepository extends CrudRepository<PasskeyCredentialEntity, Long> {

    Optional<PasskeyCredentialEntity> findByCredentialId(String credentialId);

    List<PasskeyCredentialEntity> findByUserId(Long userId);

    List<PasskeyCredentialEntity> findByUserHandle(String userHandle);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);
}
