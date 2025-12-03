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

    Optional<UserEntity> findByUsername(@NonNull @NotBlank String username);
    Optional<UserEntity> findByEmail(@NonNull @NotBlank String email);

    Optional<UserEntity> findByEmailOrUsername(@NonNull @NotBlank String email, @NonNull @NotBlank String username);
    
    Optional<UserEntity> findByUsernameAndActive(@NonNull @NotBlank String username, @NonNull Boolean active);
    Optional<UserEntity> findByEmailAndActive(@NonNull @NotBlank String email, @NonNull Boolean active);
}
