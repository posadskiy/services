package com.posadskiy.auth.web;

import io.micronaut.test.support.TestPropertyProvider;
import java.util.Map;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PostgresMicronautTest implements TestPropertyProvider {

    private static final PostgreSQLContainer POSTGRES = start();

    private static PostgreSQLContainer start() {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("auth_db")
                .withUsername("test")
                .withPassword("test");
        container.start();
        return container;
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of(
                "datasources.default.url", POSTGRES.getJdbcUrl(),
                "datasources.default.username", POSTGRES.getUsername(),
                "datasources.default.password", POSTGRES.getPassword());
    }
}
