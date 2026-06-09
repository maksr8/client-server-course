package org.example.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionProvider {
    private final HikariDataSource hikariDataSource;

    public ConnectionProvider(String jdbcURL, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcURL);
        config.setUsername(user);
        config.setPassword(password);

        hikariDataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    public void close() {
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }
}
