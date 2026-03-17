package com.estapar.parking_management.parking.domain;

/**
 * Lifecycle status of a parking session.
 */
public enum ParkingSessionStatus {
    /** Vehicle entered the garage */
    ENTERED,
    /** Vehicle parked in a spot */
    PARKED,
    /** Vehicle exited the garage */
    EXITED,
    /** Vehicle rejected at entry (garage full) */
    REJECTED
}
