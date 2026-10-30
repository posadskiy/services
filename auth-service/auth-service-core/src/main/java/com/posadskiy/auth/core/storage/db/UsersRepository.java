package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface UsersRepository extends CrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmailNormalized(String emailNormalized);

    @Query("""
        UPDATE users
        SET display_name = NULL, locale = NULL, time_zone = NULL, avatar_url = NULL
        WHERE id = :id
        """)
    void clearProfile(Long id);

    @Query("DELETE FROM product_preference WHERE user_id = :userId")
    void deletePreferences(Long userId);

    @Query("""
        INSERT INTO email_suppression (email_normalized)
        VALUES (:email)
        ON CONFLICT (email_normalized) DO NOTHING
        """)
    void suppressEmail(String email);
}
