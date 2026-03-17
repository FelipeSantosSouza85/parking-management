package com.estapar.parking_management.garage.domain;

import com.estapar.parking_management.shared.exception.GarageFullException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Domain entity representing global garage occupancy.
 * Pure POJO - no framework or persistence dependencies.
 */
public class GarageOccupancy {

    private Long id;
    private Integer totalCapacity;
    private Integer occupiedCount;
    private Instant createdAt;
    private Instant updatedAt;

    public GarageOccupancy(Integer totalCapacity) {
        if (totalCapacity == null || totalCapacity <= 0) {
            throw new IllegalArgumentException("totalCapacity must be a positive number");
        }
        this.totalCapacity = totalCapacity;
        this.occupiedCount = 0;
    }

    public void incrementOccupied() {
        if (occupiedCount >= totalCapacity) {
            throw new GarageFullException("Garage is full: " + occupiedCount + "/" + totalCapacity);
        }
        this.occupiedCount++;
    }

    public void decrementOccupied() {
        if (occupiedCount <= 0) {
            throw new IllegalStateException("Occupied count is already zero");
        }
        this.occupiedCount--;
    }

    public BigDecimal getOccupancyRate() {
        if (totalCapacity == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(occupiedCount)
                .divide(BigDecimal.valueOf(totalCapacity), 4, RoundingMode.HALF_UP);
    }

    public boolean isFull() {
        return occupiedCount >= totalCapacity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public Integer getOccupiedCount() {
        return occupiedCount;
    }

    public void setOccupiedCount(Integer occupiedCount) {
        this.occupiedCount = occupiedCount;
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
        if (!(o instanceof GarageOccupancy that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "GarageOccupancy{id=" + id + ", occupiedCount=" + occupiedCount + "/" + totalCapacity + "}";
    }
}
