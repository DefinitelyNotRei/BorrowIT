package com.borrowit.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.borrowit.dao.EquipmentDao;
import com.borrowit.dao.ReservationDao;
import com.borrowit.model.Equipment;
import com.borrowit.model.Reservation;

public class ReservationService {
    public static final int DEFAULT_BORROW_DAYS = 7;

    private final ReservationDao reservationDao;
    private final EquipmentDao equipmentDao;

    public ReservationService(ReservationDao reservationDao, EquipmentDao equipmentDao) {
        this.reservationDao = reservationDao;
        this.equipmentDao = equipmentDao;
    }

    public int createReservation(int userId, int equipmentId, int quantity)
            throws ValidationException, ServiceException {
        if (userId <= 0) {
            throw new ValidationException("A signed-in user is required.");
        }
        if (equipmentId <= 0) {
            throw new ValidationException("Select equipment to reserve.");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero.");
        }

        try {
            // Check if user has active penalty
            if (reservationDao.checkUserPenalty(userId)) {
                throw new ValidationException("You have an active penalty and cannot borrow equipment. Please try again later.");
            }
            
            Optional<Equipment> equipment = equipmentDao.findById(equipmentId);
            if (equipment.isEmpty()) {
                throw new ValidationException("Selected equipment was not found.");
            }
            if (equipment.get().getAvailableQuantity() < quantity) {
                throw new ValidationException("Not enough equipment is currently available.");
            }

            Reservation reservation = new Reservation(userId, equipmentId, quantity);
            reservation.setRemarks("Requested by borrower.");
            return reservationDao.create(reservation);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to create reservation.", exception);
        }
    }

    public boolean approveReservation(int reservationId) throws ValidationException, ServiceException {
        validateReservationId(reservationId);
        try {
            return reservationDao.approve(reservationId, DEFAULT_BORROW_DAYS, "Approved by admin.");
        } catch (SQLException exception) {
            throw new ServiceException(exception.getMessage(), exception);
        }
    }

    public boolean approveReservationWithDueDate(int reservationId, int borrowDays)
            throws ValidationException, ServiceException {
        validateReservationId(reservationId);
        if (borrowDays < 1 || borrowDays > 7) {
            throw new ValidationException("Borrow days must be between 1 and 7.");
        }
        try {
            LocalDateTime dueDate = LocalDateTime.now().plusDays(borrowDays);
            return reservationDao.approveWithDueDate(reservationId, dueDate, "Approved by admin with custom due date.");
        } catch (SQLException exception) {
            throw new ServiceException(exception.getMessage(), exception);
        }
    }

    public boolean declineReservation(int reservationId, String remarks) throws ValidationException, ServiceException {
        validateReservationId(reservationId);
        try {
            return reservationDao.decline(reservationId, cleanRemarks(remarks, "Declined by admin."));
        } catch (SQLException exception) {
            throw new ServiceException("Unable to decline reservation.", exception);
        }
    }

    public boolean cancelReservation(int reservationId, int userId) throws ValidationException, ServiceException {
        validateReservationId(reservationId);
        if (userId <= 0) {
            throw new ValidationException("A signed-in user is required.");
        }
        try {
            return reservationDao.cancelPending(reservationId, userId);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to cancel reservation.", exception);
        }
    }

    public boolean markReturned(int reservationId) throws ValidationException, ServiceException {
        validateReservationId(reservationId);
        try {
            return reservationDao.markReturned(reservationId, "Equipment returned to inventory.");
        } catch (SQLException exception) {
            throw new ServiceException(exception.getMessage(), exception);
        }
    }

    public boolean setDueDate(int reservationId, java.time.LocalDateTime dueDate) throws ValidationException, ServiceException {
        validateReservationId(reservationId);
        if (dueDate != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (dueDate.isBefore(now)) {
                throw new ValidationException("Due date cannot be in the past.");
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), dueDate.toLocalDate());
            if (days < 1 || days > 7) {
                throw new ValidationException("Due date must be between 1 and 7 days from now.");
            }
        }
        try {
            return reservationDao.setDueDate(reservationId, dueDate);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to set due date.", exception);
        }
    }

    public List<Reservation> getAllReservations() throws ServiceException {
        try {
            return reservationDao.findAllWithDetails();
        } catch (SQLException exception) {
            throw new ServiceException("Unable to load reservations.", exception);
        }
    }

    public List<Reservation> getUserReservations(int userId) throws ServiceException {
        try {
            return reservationDao.findByUserIdWithDetails(userId);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to load user reservations.", exception);
        }
    }

    public List<Reservation> getCurrentBorrowedByUser(int userId) throws ServiceException {
        try {
            return reservationDao.findCurrentBorrowedByUser(userId);
        } catch (SQLException exception) {
            throw new ServiceException("Unable to load borrowed equipment.", exception);
        }
    }

    public List<Reservation> getOverdueReservations() throws ServiceException {
        try {
            return reservationDao.findOverdue();
        } catch (SQLException exception) {
            throw new ServiceException("Unable to load overdue reservations.", exception);
        }
    }

    private void validateReservationId(int reservationId) throws ValidationException {
        if (reservationId <= 0) {
            throw new ValidationException("Select a reservation first.");
        }
    }

    private String cleanRemarks(String remarks, String fallback) {
        if (remarks == null || remarks.trim().isEmpty()) {
            return fallback;
        }
        return remarks.trim();
    }
}
