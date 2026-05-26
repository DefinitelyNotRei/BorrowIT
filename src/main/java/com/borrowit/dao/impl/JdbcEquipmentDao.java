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

import com.borrowit.dao.EquipmentDao;
import com.borrowit.database.DatabaseConnection;
import com.borrowit.model.Equipment;
import com.borrowit.model.EquipmentStatus;

public class JdbcEquipmentDao implements EquipmentDao {
    @Override
    public int create(Equipment equipment) throws SQLException {
        String sql = """
                INSERT INTO equipment (asset_tag, name, category, description, status, total_quantity, available_quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEquipment(statement, equipment);
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
    public boolean update(Equipment equipment) throws SQLException {
        String sql = """
                UPDATE equipment
                SET asset_tag = ?, name = ?, category = ?, description = ?, status = ?, total_quantity = ?, available_quantity = ?
                WHERE equipment_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEquipment(statement, equipment);
            statement.setInt(8, equipment.getEquipmentId());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int equipmentId) throws SQLException {
        String sql = "DELETE FROM equipment WHERE equipment_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipmentId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Equipment> findById(int equipmentId) throws SQLException {
        String sql = "SELECT * FROM equipment WHERE equipment_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, equipmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapEquipment(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Equipment> findAll() throws SQLException {
        return search("");
    }

    @Override
    public List<Equipment> findAvailable() throws SQLException {
        List<Equipment> equipmentList = new ArrayList<>();
        String sql = """
                SELECT *
                FROM equipment
                WHERE status = 'AVAILABLE' AND available_quantity > 0
                ORDER BY name
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                equipmentList.add(mapEquipment(resultSet));
            }
        }
        return equipmentList;
    }

    @Override
    public List<Equipment> search(String keyword) throws SQLException {
        List<Equipment> equipmentList = new ArrayList<>();
        String cleanedKeyword = keyword == null ? "" : keyword.trim();
        String sql = """
                SELECT *
                FROM equipment
                WHERE ? = ''
                   OR LOWER(asset_tag) LIKE ?
                   OR LOWER(name) LIKE ?
                   OR LOWER(category) LIKE ?
                   OR LOWER(description) LIKE ?
                   OR LOWER(status) LIKE ?
                ORDER BY name
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

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    equipmentList.add(mapEquipment(resultSet));
                }
            }
        }
        return equipmentList;
    }

    private void bindEquipment(PreparedStatement statement, Equipment equipment) throws SQLException {
        statement.setString(1, equipment.getAssetTag());
        statement.setString(2, equipment.getName());
        statement.setString(3, equipment.getCategory());
        statement.setString(4, equipment.getDescription());
        statement.setString(5, equipment.getStatus().name());
        statement.setInt(6, equipment.getTotalQuantity());
        statement.setInt(7, equipment.getAvailableQuantity());
    }

    private Equipment mapEquipment(ResultSet resultSet) throws SQLException {
        Equipment equipment = new Equipment();
        equipment.setEquipmentId(resultSet.getInt("equipment_id"));
        equipment.setAssetTag(resultSet.getString("asset_tag"));
        equipment.setName(resultSet.getString("name"));
        equipment.setCategory(resultSet.getString("category"));
        equipment.setDescription(resultSet.getString("description"));
        equipment.setStatus(EquipmentStatus.valueOf(resultSet.getString("status")));
        equipment.setTotalQuantity(resultSet.getInt("total_quantity"));
        equipment.setAvailableQuantity(resultSet.getInt("available_quantity"));
        equipment.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        equipment.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return equipment;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
