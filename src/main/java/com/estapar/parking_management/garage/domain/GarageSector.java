package com.estapar.parking_management.garage.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Entidade de domínio que representa um setor da garagem.
 * POJO puro - sem dependências de framework ou persistência.
 */
public class GarageSector {

    private Long id;
    private String sectorCode;
    private BigDecimal basePrice;
    private Integer maxCapacity;
    private LocalTime openHour;
    private LocalTime closeHour;
    private Integer durationLimitMinutes;
    private Instant createdAt;
    private Instant updatedAt;

    public GarageSector(String sectorCode, BigDecimal basePrice, Integer maxCapacity,
                        LocalTime openHour, LocalTime closeHour, Integer durationLimitMinutes) {
        this.sectorCode = Objects.requireNonNull(sectorCode, "sectorCode must not be null");
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
        this.maxCapacity = Objects.requireNonNull(maxCapacity, "maxCapacity must not be null");
        this.openHour = Objects.requireNonNull(openHour, "openHour must not be null");
        this.closeHour = Objects.requireNonNull(closeHour, "closeHour must not be null");
        this.durationLimitMinutes = Objects.requireNonNull(durationLimitMinutes, "durationLimitMinutes must not be null");
    }

    public void updateFrom(String sectorCode, BigDecimal basePrice, Integer maxCapacity,
                           LocalTime openHour, LocalTime closeHour, Integer durationLimitMinutes) {
        this.sectorCode = Objects.requireNonNull(sectorCode, "sectorCode must not be null");
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice must not be null");
        this.maxCapacity = Objects.requireNonNull(maxCapacity, "maxCapacity must not be null");
        this.openHour = Objects.requireNonNull(openHour, "openHour must not be null");
        this.closeHour = Objects.requireNonNull(closeHour, "closeHour must not be null");
        this.durationLimitMinutes = Objects.requireNonNull(durationLimitMinutes, "durationLimitMinutes must not be null");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public LocalTime getOpenHour() {
        return openHour;
    }

    public LocalTime getCloseHour() {
        return closeHour;
    }

    public Integer getDurationLimitMinutes() {
        return durationLimitMinutes;
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
        if (!(o instanceof GarageSector that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "GarageSector{id=" + id + ", sectorCode='" + sectorCode + "'}";
    }
}
