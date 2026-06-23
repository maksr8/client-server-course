package org.example.repository;

import org.example.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final ConnectionProvider connectionProvider;

    public UserRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public void createUser(User user) {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?) ON CONFLICT (username) DO NOTHING";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, user.passwordHash());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating user: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT id, username, password_hash FROM users WHERE username = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding user: " + username, e);
        }
        return null;
    }
}