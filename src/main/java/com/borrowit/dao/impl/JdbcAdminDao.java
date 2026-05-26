package com.borrowit.dao.impl;

import com.borrowit.dao.AdminDao;
import com.borrowit.database.DatabaseConnection;
import com.borrowit.model.Admin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcAdminDao implements AdminDao {
    @Override
    public Optional<Admin> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM admins WHERE user_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAdmin(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isActiveAdmin(int userId) throws SQLException {
        String sql = "SELECT 1 FROM admins WHERE user_id = ? AND is_active = 1";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Admin mapAdmin(ResultSet resultSet) throws SQLException {
        Admin admin = new Admin();
        admin.setAdminId(resultSet.getInt("admin_id"));
        admin.setUserId(resultSet.getInt("user_id"));
        admin.setStaffCode(resultSet.getString("staff_code"));
        admin.setDepartment(resultSet.getString("department"));
        admin.setActive(resultSet.getBoolean("is_active"));
        admin.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        return admin;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
