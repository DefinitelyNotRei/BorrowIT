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

import com.borrowit.dao.ReservationDao;
import com.borrowit.database.DatabaseConnection;
import com.borrowit.model.Reservation;
import com.borrowit.model.ReservationStatus;

public class JdbcReservationDao implements ReservationDao {
    private static final String SELECT_WITH_DETAILS = """
            SELECT r.*, u.full_name AS user_full_name,
                   e.name AS equipment_name, e.asset_tag
            FROM reservations r
            INNER JOIN users u ON r.user_id = u.user_id
            INNER JOIN equipment e ON r.equipment_id = e.equipment_id
            """;

    @Override
    public int create(Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservations (user_id, equipment_id, quantity, status, remarks)
                VALUES (?, ?, ?, 'PENDING', ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reservation.getUserId());
            statement.setInt(2, reservation.getEquipmentId());
            statement.setInt(3, reservation.getQuantity());
            statement.setString(4, reservation.getRemarks());
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
    public boolean approve(int reservationId, int borrowDays, String remarks) throws SQLException {
        String selectSql = """
                SELECT r.status, r.equipment_id, r.quantity, e.available_quantity
                FROM reservations r
                INNER JOIN equipment e ON r.equipment_id = e.equipment_id
                WHERE r.reservation_id = ?
                FOR UPDATE
                """;
        String updateEquipmentSql = """
                UPDATE equipment
                SET available_quantity = available_quantity - ?
                WHERE equipment_id = ? AND available_quantity >= ?
                """;
        String updateReservationSql = """
                UPDATE reservations
                SET status = 'APPROVED', approved_at = NOW(), due_date = ?, remarks = ?
                WHERE reservation_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setInt(1, reservationId);
                int equipmentId;
                int quantity;

                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Reservation was not found.");
                    }
                    ReservationStatus status = ReservationStatus.valueOf(resultSet.getString("status"));
                    if (status != ReservationStatus.PENDING) {
                        throw new SQLException("Only pending reservations can be approved.");
                    }
                    equipmentId = resultSet.getInt("equipment_id");
                    quantity = resultSet.getInt("quantity");
                    int availableQuantity = resultSet.getInt("available_quantity");
                    if (availableQuantity < quantity) {
                        throw new SQLException("Not enough equipment is available for this reservation.");
                    }
                }

                try (PreparedStatement equipmentStatement = connection.prepareStatement(updateEquipmentSql)) {
                    equipmentStatement.setInt(1, quantity);
                    equipmentStatement.setInt(2, equipmentId);
                    equipmentStatement.setInt(3, quantity);
                    if (equipmentStatement.executeUpdate() == 0) {
                        throw new SQLException("Equipment availability changed before approval.");
                    }
                }

                LocalDateTime dueDate = LocalDateTime.now().plusDays(borrowDays);
                try (PreparedStatement reservationStatement = connection.prepareStatement(updateReservationSql)) {
                    reservationStatement.setTimestamp(1, Timestamp.valueOf(dueDate));
                    reservationStatement.setString(2, remarks);
                    reservationStatement.setInt(3, reservationId);
                    reservationStatement.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean approveWithDueDate(int reservationId, LocalDateTime dueDate, String remarks) throws SQLException {
        String selectSql = """
                SELECT r.status, r.equipment_id, r.quantity, e.available_quantity
                FROM reservations r
                INNER JOIN equipment e ON r.equipment_id = e.equipment_id
                WHERE r.reservation_id = ?
                FOR UPDATE
                """;
        String updateEquipmentSql = """
                UPDATE equipment
                SET available_quantity = available_quantity - ?
                WHERE equipment_id = ? AND available_quantity >= ?
                """;
        String updateReservationSql = """
                UPDATE reservations
                SET status = 'APPROVED', approved_at = NOW(), due_date = ?, remarks = ?
                WHERE reservation_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setInt(1, reservationId);
                int equipmentId;
                int quantity;

                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Reservation was not found.");
                    }
                    ReservationStatus status = ReservationStatus.valueOf(resultSet.getString("status"));
                    if (status != ReservationStatus.PENDING) {
                        throw new SQLException("Only pending reservations can be approved.");
                    }
                    equipmentId = resultSet.getInt("equipment_id");
                    quantity = resultSet.getInt("quantity");
                    int availableQuantity = resultSet.getInt("available_quantity");
                    if (availableQuantity < quantity) {
                        throw new SQLException("Not enough equipment is available for this reservation.");
                    }
                }

                try (PreparedStatement equipmentStatement = connection.prepareStatement(updateEquipmentSql)) {
                    equipmentStatement.setInt(1, quantity);
                    equipmentStatement.setInt(2, equipmentId);
                    equipmentStatement.setInt(3, quantity);
                    if (equipmentStatement.executeUpdate() == 0) {
                        throw new SQLException("Equipment availability changed before approval.");
                    }
                }

                try (PreparedStatement reservationStatement = connection.prepareStatement(updateReservationSql)) {
                    reservationStatement.setTimestamp(1, Timestamp.valueOf(dueDate));
                    reservationStatement.setString(2, remarks);
                    reservationStatement.setInt(3, reservationId);
                    reservationStatement.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean decline(int reservationId, String remarks) throws SQLException {
        String sql = """
                UPDATE reservations
                SET status = 'DECLINED', remarks = ?
                WHERE reservation_id = ? AND status = 'PENDING'
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, remarks);
            statement.setInt(2, reservationId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean cancelPending(int reservationId, int userId) throws SQLException {
        String sql = """
                UPDATE reservations
                SET status = 'CANCELLED', remarks = 'Cancelled by borrower.'
                WHERE reservation_id = ? AND user_id = ? AND status = 'PENDING'
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            statement.setInt(2, userId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean markReturned(int reservationId, String remarks) throws SQLException {
        String selectSql = """
                SELECT status, equipment_id, quantity, user_id, due_date
                FROM reservations
                WHERE reservation_id = ?
                FOR UPDATE
                """;
        String updateEquipmentSql = """
                UPDATE equipment
                SET available_quantity =
                    CASE
                        WHEN available_quantity + ? > total_quantity THEN total_quantity
                        ELSE available_quantity + ?
                    END
                WHERE equipment_id = ?
                """;
        String updateReservationSql = """
                UPDATE reservations
                SET status = 'RETURNED', returned_at = NOW(), remarks = ?, is_late = ?, penalty_end_date = ?
                WHERE reservation_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                selectStatement.setInt(1, reservationId);
                int equipmentId;
                int quantity;
                LocalDateTime dueDate;
                boolean isLate = false;
                LocalDateTime penaltyEndDate = null;

                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Reservation was not found.");
                    }
                    ReservationStatus status = ReservationStatus.valueOf(resultSet.getString("status"));
                    if (status != ReservationStatus.APPROVED) {
                        throw new SQLException("Only approved reservations can be marked as returned.");
                    }
                    equipmentId = resultSet.getInt("equipment_id");
                    quantity = resultSet.getInt("quantity");
                    dueDate = toLocalDateTime(resultSet.getTimestamp("due_date"));
                    
                    // Check if return is late
                    if (dueDate != null && LocalDateTime.now().isAfter(dueDate)) {
                        isLate = true;
                        penaltyEndDate = LocalDateTime.now().plusDays(7);
                    }
                }

                try (PreparedStatement equipmentStatement = connection.prepareStatement(updateEquipmentSql)) {
                    equipmentStatement.setInt(1, quantity);
                    equipmentStatement.setInt(2, quantity);
                    equipmentStatement.setInt(3, equipmentId);
                    equipmentStatement.executeUpdate();
                }

                try (PreparedStatement reservationStatement = connection.prepareStatement(updateReservationSql)) {
                    reservationStatement.setString(1, remarks);
                    reservationStatement.setBoolean(2, isLate);
                    reservationStatement.setTimestamp(3, penaltyEndDate == null ? null : Timestamp.valueOf(penaltyEndDate));
                    reservationStatement.setInt(4, reservationId);
                    reservationStatement.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public boolean setDueDate(int reservationId, java.time.LocalDateTime dueDate) throws SQLException {
        String sql = """
                UPDATE reservations
                SET due_date = ?
                WHERE reservation_id = ? AND status = 'APPROVED'
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, dueDate == null ? null : Timestamp.valueOf(dueDate));
            statement.setInt(2, reservationId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Reservation> findById(int reservationId) throws SQLException {
        String sql = SELECT_WITH_DETAILS + " WHERE r.reservation_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapReservation(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Reservation> findAllWithDetails() throws SQLException {
        String sql = SELECT_WITH_DETAILS + " ORDER BY r.request_date DESC";
        return queryReservations(sql);
    }

    @Override
    public List<Reservation> findByUserIdWithDetails(int userId) throws SQLException {
        String sql = SELECT_WITH_DETAILS + " WHERE r.user_id = ? ORDER BY r.request_date DESC";
        return queryReservations(sql, userId);
    }

    @Override
    public List<Reservation> findCurrentBorrowedByUser(int userId) throws SQLException {
        String sql = SELECT_WITH_DETAILS + """
                 WHERE r.user_id = ? AND r.status = 'APPROVED'
                 ORDER BY r.due_date ASC
                """;
        return queryReservations(sql, userId);
    }

    @Override
    public List<Reservation> findOverdue() throws SQLException {
        String sql = SELECT_WITH_DETAILS + """
                 WHERE r.status = 'APPROVED' AND r.due_date < NOW()
                 ORDER BY r.due_date ASC
                """;
        return queryReservations(sql);
    }

    private List<Reservation> queryReservations(String sql, int... parameters) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setInt(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reservations.add(mapReservation(resultSet));
                }
            }
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet resultSet) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setReservationId(resultSet.getInt("reservation_id"));
        reservation.setUserId(resultSet.getInt("user_id"));
        reservation.setEquipmentId(resultSet.getInt("equipment_id"));
        reservation.setQuantity(resultSet.getInt("quantity"));
        reservation.setStatus(ReservationStatus.valueOf(resultSet.getString("status")));
        reservation.setRequestDate(toLocalDateTime(resultSet.getTimestamp("request_date")));
        reservation.setApprovedAt(toLocalDateTime(resultSet.getTimestamp("approved_at")));
        reservation.setDueDate(toLocalDateTime(resultSet.getTimestamp("due_date")));
        reservation.setReturnDate(toLocalDateTime(resultSet.getTimestamp("return_date")));
        reservation.setReturnedAt(toLocalDateTime(resultSet.getTimestamp("returned_at")));
        reservation.setLate(resultSet.getBoolean("is_late"));
        reservation.setPenaltyEndDate(toLocalDateTime(resultSet.getTimestamp("penalty_end_date")));
        reservation.setRemarks(resultSet.getString("remarks"));
        reservation.setUserFullName(resultSet.getString("user_full_name"));
        reservation.setEquipmentName(resultSet.getString("equipment_name"));
        reservation.setAssetTag(resultSet.getString("asset_tag"));
        return reservation;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    @Override
    public boolean checkUserPenalty(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM reservations
                WHERE user_id = ? AND penalty_end_date > NOW() AND status = 'RETURNED'
                """;
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void rollback(Connection connection) throws SQLException {
        if (connection != null) {
            connection.rollback();
        }
    }
}
