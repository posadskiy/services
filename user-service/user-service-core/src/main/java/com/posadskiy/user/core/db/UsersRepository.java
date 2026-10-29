package com.posadskiy.user.core.db;

import com.posadskiy.user.core.db.entity.UserEntity;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface UsersRepository extends CrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(@NonNull @NotBlank String email);

    Optional<UserEntity> findByEmailAndActive(@NonNull @NotBlank String email, @NonNull Boolean active);
}
