package com.borrowit.dao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.borrowit.model.Reservation;

public interface ReservationDao {
    int create(Reservation reservation) throws SQLException;

    boolean approve(int reservationId, int borrowDays, String remarks) throws SQLException;

    boolean approveWithDueDate(int reservationId, LocalDateTime dueDate, String remarks) throws SQLException;

    boolean decline(int reservationId, String remarks) throws SQLException;

    boolean cancelPending(int reservationId, int userId) throws SQLException;

    boolean markReturned(int reservationId, String remarks) throws SQLException;

    boolean setDueDate(int reservationId, java.time.LocalDateTime dueDate) throws SQLException;

    Optional<Reservation> findById(int reservationId) throws SQLException;

    List<Reservation> findAllWithDetails() throws SQLException;

    List<Reservation> findByUserIdWithDetails(int userId) throws SQLException;

    List<Reservation> findCurrentBorrowedByUser(int userId) throws SQLException;

    List<Reservation> findOverdue() throws SQLException;

    boolean checkUserPenalty(int userId) throws SQLException;
}
