package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface UsersRepository extends CrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailNormalized(String emailNormalized);

    Optional<UserEntity> findByUsername(String username);
}
