package com.estapar.parking_management.parking.domain;

import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a parking session.
 * Pure POJO - no framework or persistence dependencies.
 * State transitions are controlled in the application layer.
 */
public class ParkingSession {

    private Long id;
    private String licensePlate;
    private ParkingSessionStatus status;
    private Instant entryTime;
    private Instant parkedTime;
    private Instant exitTime;
    private ParkingSpot parkingSpot;
    private GarageSector sector;
    private BigDecimal occupancyRateAtEntry;
    private BigDecimal priceAdjustmentRateAtEntry;
    private BigDecimal hourlyPriceApplied;
    private BigDecimal chargedAmount;
    private Instant createdAt;
    private Instant updatedAt;

    public ParkingSession(String licensePlate, Instant entryTime, ParkingSessionStatus status) {
        this.licensePlate = Objects.requireNonNull(licensePlate, "licensePlate must not be null");
        if (licensePlate.isBlank()) {
            throw new IllegalArgumentException("licensePlate must not be blank");
        }
        this.entryTime = Objects.requireNonNull(entryTime, "entryTime must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public ParkingSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingSessionStatus status) {
        this.status = status;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public Instant getParkedTime() {
        return parkedTime;
    }

    public void setParkedTime(Instant parkedTime) {
        this.parkedTime = parkedTime;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public void setExitTime(Instant exitTime) {
        this.exitTime = exitTime;
    }

    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }

    public void setParkingSpot(ParkingSpot parkingSpot) {
        this.parkingSpot = parkingSpot;
    }

    public GarageSector getSector() {
        return sector;
    }

    public void setSector(GarageSector sector) {
        this.sector = sector;
    }

    public BigDecimal getOccupancyRateAtEntry() {
        return occupancyRateAtEntry;
    }

    public void setOccupancyRateAtEntry(BigDecimal occupancyRateAtEntry) {
        this.occupancyRateAtEntry = occupancyRateAtEntry;
    }

    public BigDecimal getPriceAdjustmentRateAtEntry() {
        return priceAdjustmentRateAtEntry;
    }

    public void setPriceAdjustmentRateAtEntry(BigDecimal priceAdjustmentRateAtEntry) {
        this.priceAdjustmentRateAtEntry = priceAdjustmentRateAtEntry;
    }

    public BigDecimal getHourlyPriceApplied() {
        return hourlyPriceApplied;
    }

    public void setHourlyPriceApplied(BigDecimal hourlyPriceApplied) {
        this.hourlyPriceApplied = hourlyPriceApplied;
    }

    public BigDecimal getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(BigDecimal chargedAmount) {
        this.chargedAmount = chargedAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParkingSession that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ParkingSession{id=" + id + ", licensePlate='" + licensePlate + "', status=" + status + "}";
    }
}
