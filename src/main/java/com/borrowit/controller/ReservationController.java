package com.borrowit.controller;

import java.util.List;

import com.borrowit.dao.impl.JdbcEquipmentDao;
import com.borrowit.dao.impl.JdbcReservationDao;
import com.borrowit.model.Reservation;
import com.borrowit.service.ReservationService;
import com.borrowit.service.ServiceException;
import com.borrowit.service.ValidationException;

public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController() {
        this.reservationService = new ReservationService(new JdbcReservationDao(), new JdbcEquipmentDao());
    }

    public int createReservation(int userId, int equipmentId, int quantity)
            throws ValidationException, ServiceException {
        return reservationService.createReservation(userId, equipmentId, quantity);
    }

    public boolean approveReservation(int reservationId) throws ValidationException, ServiceException {
        return reservationService.approveReservation(reservationId);
    }

    public boolean approveReservationWithDueDate(int reservationId, int borrowDays)
            throws ValidationException, ServiceException {
        return reservationService.approveReservationWithDueDate(reservationId, borrowDays);
    }

    public boolean declineReservation(int reservationId, String remarks)
            throws ValidationException, ServiceException {
        return reservationService.declineReservation(reservationId, remarks);
    }

    public boolean cancelReservation(int reservationId, int userId)
            throws ValidationException, ServiceException {
        return reservationService.cancelReservation(reservationId, userId);
    }

    public boolean markReturned(int reservationId) throws ValidationException, ServiceException {
        return reservationService.markReturned(reservationId);
    }

    public boolean setDueDate(int reservationId, java.time.LocalDateTime dueDate)
            throws ValidationException, ServiceException {
        return reservationService.setDueDate(reservationId, dueDate);
    }

    public List<Reservation> getAllReservations() throws ServiceException {
        return reservationService.getAllReservations();
    }

    public List<Reservation> getUserReservations(int userId) throws ServiceException {
        return reservationService.getUserReservations(userId);
    }

    public List<Reservation> getCurrentBorrowedByUser(int userId) throws ServiceException {
        return reservationService.getCurrentBorrowedByUser(userId);
    }

    public List<Reservation> getOverdueReservations() throws ServiceException {
        return reservationService.getOverdueReservations();
    }
}
