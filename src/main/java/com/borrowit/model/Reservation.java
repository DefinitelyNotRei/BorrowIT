package com.borrowit.model;

import java.time.LocalDateTime;

public class Reservation {
    private int reservationId;
    private int userId;
    private int equipmentId;
    private int quantity;
    private ReservationStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime approvedAt;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private LocalDateTime returnedAt;
    private boolean late;
    private LocalDateTime penaltyEndDate;
    private String remarks;
    private String userFullName;
    private String equipmentName;
    private String assetTag;

    public Reservation() {
    }

    public Reservation(int userId, int equipmentId, int quantity) {
        this.userId = userId;
        this.equipmentId = equipmentId;
        this.quantity = quantity;
        this.status = ReservationStatus.PENDING;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public boolean isLate() {
        return late;
    }

    public void setLate(boolean late) {
        this.late = late;
    }

    public LocalDateTime getPenaltyEndDate() {
        return penaltyEndDate;
    }

    public void setPenaltyEndDate(LocalDateTime penaltyEndDate) {
        this.penaltyEndDate = penaltyEndDate;
    }
}
