package com.estapar.parking_management.garage.domain;

import com.estapar.parking_management.shared.exception.SpotAlreadyOccupiedException;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidade de domínio que representa uma vaga de estacionamento.
 * POJO puro - sem dependências de framework ou persistência.
 */
public class ParkingSpot {

    private Long id;
    private Integer externalSpotId;
    private GarageSector sector;
    private Double lat;
    private Double lng;
    private boolean occupied;
    private Instant createdAt;
    private Instant updatedAt;

    public ParkingSpot(Integer externalSpotId, GarageSector sector, Double lat, Double lng, boolean occupied) {
        this.externalSpotId = Objects.requireNonNull(externalSpotId, "externalSpotId must not be null");
        this.sector = Objects.requireNonNull(sector, "sector must not be null");
        this.lat = Objects.requireNonNull(lat, "lat must not be null");
        this.lng = Objects.requireNonNull(lng, "lng must not be null");
        this.occupied = occupied;
    }

    public void updateFrom(GarageSector sector, Double lat, Double lng, boolean occupied) {
        this.sector = Objects.requireNonNull(sector, "sector must not be null");
        this.lat = Objects.requireNonNull(lat, "lat must not be null");
        this.lng = Objects.requireNonNull(lng, "lng must not be null");
        this.occupied = occupied;
    }

    public void occupy() {
        if (this.occupied) {
            throw new SpotAlreadyOccupiedException("Spot " + externalSpotId + " is already occupied");
        }
        this.occupied = true;
    }

    public void release() {
        if (!this.occupied) {
            throw new IllegalStateException("Spot " + externalSpotId + " is not occupied");
        }
        this.occupied = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getExternalSpotId() {
        return externalSpotId;
    }

    public GarageSector getSector() {
        return sector;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public boolean isOccupied() {
        return occupied;
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
        if (!(o instanceof ParkingSpot that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ParkingSpot{id=" + id + ", externalSpotId=" + externalSpotId + ", occupied=" + occupied + "}";
    }
}
