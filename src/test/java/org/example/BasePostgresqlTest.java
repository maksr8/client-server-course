package org.example;

import org.example.repository.ConnectionProvider;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class BasePostgresqlTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:18.3-alpine")
    )
            .withDatabaseName("cs_storage_db")
            .withUsername("root")
            .withPassword("root");

    static ConnectionProvider connectionProvider;

    @BeforeAll
    static void startContainerAndMigrate() {
        postgres.start();

        String dbUrl = postgres.getJdbcUrl();
        String dbUser = postgres.getUsername();
        String dbPass = postgres.getPassword();

        System.out.println("Running database migrations...");
        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPass)
                .load();
        flyway.migrate();
        System.out.println("Migrations applied successfully");

        connectionProvider = new ConnectionProvider(dbUrl, dbUser, dbPass);
    }

    @AfterAll
    static void closeProvider() {
        if (connectionProvider != null) {
            connectionProvider.close();
        }
    }
}