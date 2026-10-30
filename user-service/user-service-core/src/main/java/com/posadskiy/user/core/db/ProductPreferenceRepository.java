package com.posadskiy.user.core.db;

import com.posadskiy.user.core.db.entity.ProductPreferenceEntity;
import com.posadskiy.user.core.db.entity.ProductPreferenceId;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface ProductPreferenceRepository extends CrudRepository<ProductPreferenceEntity, ProductPreferenceId> {
}
