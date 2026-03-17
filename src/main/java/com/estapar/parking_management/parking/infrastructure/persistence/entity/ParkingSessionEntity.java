package com.estapar.parking_management.parking.infrastructure.persistence.entity;

import com.estapar.parking_management.garage.infrastructure.persistence.entity.GarageSectorEntity;
import com.estapar.parking_management.garage.infrastructure.persistence.entity.ParkingSpotEntity;
import com.estapar.parking_management.parking.domain.ParkingSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "parking_session",
    indexes = {
        @Index(name = "idx_session_plate_status", columnList = "license_plate, status"),
        @Index(name = "idx_session_sector_exit", columnList = "sector_id, exit_time")
    }
)
public class ParkingSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingSessionStatus status;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "parked_time")
    private Instant parkedTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_spot_id")
    private ParkingSpotEntity parkingSpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private GarageSectorEntity sector;

    @Column(name = "occupancy_rate_at_entry", precision = 10, scale = 2)
    private BigDecimal occupancyRateAtEntry;

    @Column(name = "price_adjustment_rate_at_entry", precision = 10, scale = 2)
    private BigDecimal priceAdjustmentRateAtEntry;

    @Column(name = "hourly_price_applied", precision = 10, scale = 2)
    private BigDecimal hourlyPriceApplied;

    @Column(name = "charged_amount", precision = 10, scale = 2)
    private BigDecimal chargedAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public ParkingSessionEntity() {
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

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
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

    public void setEntryTime(Instant entryTime) {
        this.entryTime = entryTime;
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

    public ParkingSpotEntity getParkingSpot() {
        return parkingSpot;
    }

    public void setParkingSpot(ParkingSpotEntity parkingSpot) {
        this.parkingSpot = parkingSpot;
    }

    public GarageSectorEntity getSector() {
        return sector;
    }

    public void setSector(GarageSectorEntity sector) {
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
}
