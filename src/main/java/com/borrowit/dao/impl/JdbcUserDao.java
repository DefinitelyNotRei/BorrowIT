package com.borrowit.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.borrowit.dao.UserDao;
import com.borrowit.database.DatabaseConnection;
import com.borrowit.model.User;
import com.borrowit.model.UserRole;

public class JdbcUserDao implements UserDao {
    @Override
    public int create(User user) throws SQLException {
        String sql = """
                INSERT INTO users (full_name, username, email, branch, course, block, year_level, phone_number, password_hash, role, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getBranch());
            statement.setString(5, user.getCourse());
            statement.setString(6, user.getBlock());
            statement.setInt(7, user.getYearLevel());
            statement.setString(8, user.getPhoneNumber());
            statement.setString(9, user.getPasswordHash());
            statement.setString(10, user.getRole().name());
            statement.setBoolean(11, user.isActive());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean usernameExists(String username) throws SQLException {
        return exists("SELECT 1 FROM users WHERE username = ?", username);
    }

    @Override
    public boolean emailExists(String email) throws SQLException {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }

    @Override
    public List<User> findAll() throws SQLException {
        return search("");
    }

    @Override
    public List<User> search(String keyword) throws SQLException {
        List<User> users = new ArrayList<>();
        String cleanedKeyword = keyword == null ? "" : keyword.trim();
        String sql = """
                SELECT *
                FROM users
                WHERE ? = ''
                   OR LOWER(full_name) LIKE ?
                   OR LOWER(SUBSTRING_INDEX(full_name, ' ', -1)) LIKE ?
                   OR LOWER(username) LIKE ?
                   OR LOWER(email) LIKE ?
                   OR LOWER(role) LIKE ?
                   OR LOWER(branch) LIKE ?
                   OR LOWER(course) LIKE ?
                   OR LOWER(block) LIKE ?
                   OR CAST(year_level AS CHAR) LIKE ?
                   OR LOWER(phone_number) LIKE ?
                ORDER BY username ASC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            String likeKeyword = "%" + cleanedKeyword.toLowerCase() + "%";
            statement.setString(1, cleanedKeyword);
            statement.setString(2, likeKeyword);
            statement.setString(3, likeKeyword);
            statement.setString(4, likeKeyword);
            statement.setString(5, likeKeyword);
            statement.setString(6, likeKeyword);
            statement.setString(7, likeKeyword);
            statement.setString(8, likeKeyword);
            statement.setString(9, likeKeyword);
            statement.setString(10, likeKeyword);
            statement.setString(11, likeKeyword);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapUser(resultSet));
                }
            }
        }
        return users;
    }

    @Override
    public boolean updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, branch = ?, course = ?, block = ?, year_level = ?, phone_number = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getBranch());
            statement.setString(4, user.getCourse());
            statement.setString(5, user.getBlock());
            statement.setInt(6, user.getYearLevel());
            statement.setString(7, user.getPhoneNumber());
            statement.setInt(8, user.getUserId());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deactivate(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            return statement.executeUpdate() > 0;
        }
    }

    private boolean exists(String sql, String value) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getInt("user_id"));
        user.setFullName(resultSet.getString("full_name"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        if (hasColumn(resultSet, "branch")) {
            user.setBranch(resultSet.getString("branch"));
        } else {
            user.setBranch("General");
        }
        if (hasColumn(resultSet, "course")) {
            user.setCourse(resultSet.getString("course"));
        } else {
            user.setCourse("General");
        }
        if (hasColumn(resultSet, "block")) {
            user.setBlock(resultSet.getString("block"));
        } else {
            user.setBlock("A");
        }
        if (hasColumn(resultSet, "year_level")) {
            user.setYearLevel(resultSet.getInt("year_level"));
        } else {
            user.setYearLevel(1);
        }
        if (hasColumn(resultSet, "phone_number")) {
            user.setPhoneNumber(resultSet.getString("phone_number"));
        } else {
            user.setPhoneNumber("");
        }
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(parseRole(resultSet.getString("role")));
        user.setActive(resultSet.getBoolean("is_active"));
        user.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        user.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return user;
    }

    private UserRole parseRole(String roleValue) {
        if (roleValue == null || roleValue.isBlank()) {
            return UserRole.USER;
        }
        try {
            return UserRole.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return UserRole.USER;
        }
    }

    private boolean hasColumn(ResultSet resultSet, String columnName) {
        try {
            resultSet.findColumn(columnName);
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
