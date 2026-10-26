package com.posadskiy.auth.core.storage.db;

import com.posadskiy.auth.core.storage.db.entity.AuditEventEntity;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import static io.micronaut.data.model.query.builder.sql.Dialect.POSTGRES;

@JdbcRepository(dialect = POSTGRES)
public interface AuditEventRepository extends CrudRepository<AuditEventEntity, Long> {
}
