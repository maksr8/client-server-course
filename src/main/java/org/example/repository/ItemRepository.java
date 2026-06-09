package org.example.repository;

import org.example.dto.ItemFilter;
import org.example.model.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemRepository {

    private final ConnectionProvider connectionProvider;

    public ItemRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Item findById(int id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToItem(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding item: " + id, e);
        }
        return null;
    }

    public boolean reduceQuantity(int id, int amount) {
        String sql = "UPDATE items SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, id);
            stmt.setInt(3, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while reducing stock", e);
        }
    }

    public boolean addQuantity(int id, int amount) {
        String sql = "UPDATE items SET quantity = quantity + ? WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, amount);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while adding stock", e);
        }
    }

    public int create(Item item) {
        String sql = "INSERT INTO items (name, category, price, quantity) VALUES (?, ?, ?, ?)";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setDouble(3, item.getPrice());
            stmt.setInt(4, item.getQuantity());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating item failed");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while saving item", e);
        }
    }

    public boolean update(Item item) {
        String sql = "UPDATE items SET name = ?, category = ?, price = ?, quantity = ? WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.getName());
            stmt.setString(2, item.getCategory());
            stmt.setDouble(3, item.getPrice());
            stmt.setInt(4, item.getQuantity());
            stmt.setInt(5, item.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while updating item", e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while deleting item", e);
        }
    }

    public List<Item> search(ItemFilter filter) {
        List<Object> params = new ArrayList<>();

        String whereConditions = Stream.of(
                        stringEquals("name", filter.name(), params),
                        stringEquals("category", filter.category(), params),
                        numberGreaterOrEquals("price", filter.minPrice(), params),
                        numberLessOrEquals("price", filter.maxPrice(), params),
                        numberGreaterOrEquals("quantity", filter.minQuantity(), params),
                        numberLessOrEquals("quantity", filter.maxQuantity(), params)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" AND "));

        String sql = "SELECT * FROM items";
        if (!whereConditions.isEmpty()) {
            sql += " WHERE " + whereConditions;
        }
        sql += " ORDER BY id LIMIT ? OFFSET ?";
        params.add(filter.limit());
        params.add(filter.offset());

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<Item> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapRowToItem(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while searching items", e);
        }
    }

    private String stringEquals(String columnName, String value, List<Object> params) {
        if (value == null || value.isBlank()) {
            return null;
        }
        params.add(value);
        return columnName + " = ?";
    }

    private String numberGreaterOrEquals(String columnName, Number value, List<Object> params) {
        if (value == null) {
            return null;
        }
        params.add(value);
        return columnName + " >= ?";
    }

    private String numberLessOrEquals(String columnName, Number value, List<Object> params) {
        if (value == null) {
            return null;
        }
        params.add(value);
        return columnName + " <= ?";
    }

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        return new Item(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getDouble("price"),
                rs.getInt("quantity")
        );
    }
}