package com.posadskiy.auth.core.storage.db;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

import com.posadskiy.auth.core.storage.db.entity.MailOutboxEntity;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import java.time.Instant;
import java.util.List;

@JdbcRepository(dialect = POSTGRES)
public interface MailOutboxRepository extends CrudRepository<MailOutboxEntity, Long> {

    List<MailOutboxEntity> findTop20ByAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
            int attempts, Instant now);

    @Query("DELETE FROM mail_outbox WHERE created_at < :before")
    void deleteCreatedBefore(Instant before);
}
