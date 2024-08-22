package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface UsersRepository extends CrudRepository<UserEntity, Long> {

    @Transactional
    UserEntity save(@NonNull @NotBlank String username,
                    @NonNull @NotBlank String email,
                    @NonNull @NotNull String passwordHash);

    Optional<UserEntity> findByUsername(@NonNull @NotBlank String username);

    Optional<UserEntity> findByEmailOrUsername(@NonNull @NotBlank String email, @NonNull @NotBlank String username);
}
