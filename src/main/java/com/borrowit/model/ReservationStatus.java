package com.borrowit.model;

public enum ReservationStatus {
    PENDING,
    APPROVED,
    DECLINED,
    RETURNED,
    CANCELLED;

    public boolean isFinalStatus() {
        return this == DECLINED || this == RETURNED || this == CANCELLED;
    }
}
