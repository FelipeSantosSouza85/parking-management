package com.estapar.parking_management.garage.application;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.application.port.GarageSectorPort;
import com.estapar.parking_management.garage.application.port.ParkingSpotPort;
import com.estapar.parking_management.garage.domain.GarageOccupancy;
import com.estapar.parking_management.garage.domain.GarageSector;
import com.estapar.parking_management.garage.domain.ParkingSpot;
import com.estapar.parking_management.garage.infrastructure.client.GarageSimulatorClient;
import com.estapar.parking_management.garage.infrastructure.client.dto.GarageConfigurationResponse;
import com.estapar.parking_management.garage.infrastructure.client.dto.SectorResponse;
import com.estapar.parking_management.garage.infrastructure.client.dto.SpotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for synchronizing garage state with the external simulator.
 * Fetches configuration (sectors and spots) from the simulator, persists or updates data
 * locally, and keeps the occupancy aggregate (GarageOccupancy) up to date.
 * Uses batch operations to optimize the number of database round-trips.
 */
@Service
public class GarageSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(GarageSynchronizationService.class);
    /** Expected format for times (e.g. "08:00", "22:00"). */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final GarageSimulatorClient simulatorClient;
    private final GarageSectorPort sectorPort;
    private final ParkingSpotPort spotPort;
    private final GarageOccupancyPort occupancyPort;

    /**
     * Builds the synchronization service with required dependencies.
     *
     * @param simulatorClient client to fetch simulator configuration
     * @param sectorPort     port for sector persistence
     * @param spotPort       port for spot persistence
     * @param occupancyPort  port for occupancy aggregate persistence
     */
    public GarageSynchronizationService(
            GarageSimulatorClient simulatorClient,
            GarageSectorPort sectorPort,
            ParkingSpotPort spotPort,
            GarageOccupancyPort occupancyPort
    ) {
        this.simulatorClient = simulatorClient;
        this.sectorPort = sectorPort;
        this.spotPort = spotPort;
        this.occupancyPort = occupancyPort;
    }

    /**
     * Synchronizes the full garage state with the simulator.
     * Flow: (1) fetch simulator configuration, (2) synchronize sectors in batch,
     * (3) synchronize spots in batch, (4) update occupancy aggregate.
     *
     * @throws IllegalArgumentException if configuration does not contain at least one sector
     */
    @Transactional
    public void synchronize() {
        log.info("[GARAGE] - [SYNC_START]");

        GarageConfigurationResponse config = simulatorClient.fetchGarageConfiguration();

        Map<String, GarageSector> sectorsBySectorCode = synchronizeSectors(config.garage());
        Long spotsOccupied = synchronizeSpots(config.spots(), sectorsBySectorCode);
        int totalCapacity = calculateTotalCapacity(sectorsBySectorCode);

        synchronizeOccupancy(totalCapacity, spotsOccupied);

        log.info("[GARAGE] - [SYNC_COMPLETED] sectors={}, spots={}, occupied={}/{}",
                sectorsBySectorCode.size(), totalCapacity, spotsOccupied, totalCapacity);
    }

    /**
     * Synchronizes sectors: loads existing in batch, updates or creates, persists in batch.
     *
     * @param sectors list of sectors returned by the simulator
     * @return map of sector code → persisted sector
     */
    private Map<String, GarageSector> synchronizeSectors(List<SectorResponse> sectors) {
        if (sectors.isEmpty()) {
            throw new IllegalArgumentException("Garage configuration must contain at least one sector");
        }

        List<String> sectorCodes = sectors.stream().map(SectorResponse::sector).toList();
        
        Map<String, GarageSector> mapSectorByCode = findAndcreateMapSectorsByCode(sectorCodes);

        List<GarageSector> sectorToSave = buildSectors(sectors, mapSectorByCode);

        List<GarageSector> saved = sectorPort.saveAll(sectorToSave);
        
        Map<String, GarageSector> result = saved.stream()
                .collect(Collectors.toMap(GarageSector::getSectorCode, Function.identity()));

        log.debug("[GARAGE] - [SYNC_DEBUG] sectors={}", result.size());
        return result;
    }

    /**
     * Fetches existing sectors by codes and builds a map.
     *
     * @param sectorCodes sector codes to look up
     * @return map of code → sector (empty for non-existent sectors)
     */
    private Map<String, GarageSector> findAndcreateMapSectorsByCode(List<String> sectorCodes) {
        return sectorPort.findAllBySectorCodeIn(sectorCodes)
                .stream()
                .collect(Collectors.toMap(GarageSector::getSectorCode, Function.identity()));
    }

    /**
     * Builds the list of sectors for persistence: updates existing or creates new ones.
     *
     * @param sectors simulator responses
     * @param mapSectorByCode sectors already in the database, indexed by code
     * @return list ready for saveAll
     */
    private List<GarageSector> buildSectors(List<SectorResponse> sectors, Map<String, GarageSector> mapSectorByCode) {
        return sectors.stream()
        .map(r -> {
            GarageSector existing = mapSectorByCode.get(r.sector());
            if (existing != null) {
                existing.updateFrom(
                        r.sector(), r.basePrice(), r.maxCapacity(),
                        parseTime(r.openHour()), parseTime(r.closeHour()),
                        r.durationLimitMinutes()
                );
                return existing;
            }
            return new GarageSector(
                    r.sector(), r.basePrice(), r.maxCapacity(),
                    parseTime(r.openHour()), parseTime(r.closeHour()),
                    r.durationLimitMinutes()
            );
        })
        .toList();
    }

    /**
     * Synchronizes spots: filters invalid ones, loads existing in batch, updates or creates, persists in batch.
     *
     * @param spots spots returned by the simulator
     * @param sectorsBySectorCode  already synchronized sectors (required for reference)
     * @return number of occupied spots after synchronization
     */
    private Long synchronizeSpots(List<SpotResponse> spots, Map<String, GarageSector> sectorsBySectorCode) {
        if (spots.isEmpty()) {
            log.warn("[GARAGE] - [SYNC_WARN] no spots");
            return 0L;
        }

        List<SpotResponse> validSpots = validateSpots(spots, sectorsBySectorCode);

        if (validSpots.isEmpty()) {
            return 0L;
        }

        List<Integer> spotIds = validSpots.stream().map(SpotResponse::id).toList();

        Map<Integer, ParkingSpot> mapSpotById = findAndcreateMapSpotsById(spotIds);

        List<ParkingSpot> spotsToSave = validSpots.stream()
                .map(r -> buildOrUpdateSpot(r, sectorsBySectorCode.get(r.sector()), mapSpotById))
                .toList();

        List<ParkingSpot> saved = spotPort.saveAll(spotsToSave);

        return saved.stream().filter(ParkingSpot::isOccupied).count();
    }

    /**
     * Filters spots that reference non-existent sectors (ignored with warning log).
     *
     * @param spots spots to validate
     * @param sectorsBySectorCode  known sectors
     * @return only spots whose sector exists
     */
    private List<SpotResponse> validateSpots(List<SpotResponse> spots, Map<String, GarageSector> sectorsBySectorCode) {
        return spots.stream()
                .filter(response -> {
                    if (!sectorsBySectorCode.containsKey(response.sector())) {
                        log.warn("[GARAGE] - [SYNC_WARN] spot {} references unknown sector: {}", response.id(), response.sector());
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    /**
     * Fetches existing spots by external IDs and builds a lookup map.
     *
     * @param spotIds external spot IDs (from simulator)
     * @return map of externalSpotId → spot
     */
    private Map<Integer, ParkingSpot> findAndcreateMapSpotsById(List<Integer> spotIds) {
        return spotPort.findAllByExternalSpotIdIn(spotIds).stream()
                .collect(Collectors.toMap(ParkingSpot::getExternalSpotId, Function.identity()));
    }

    /**
     * Creates a new spot or updates an existing one with simulator data.
     *
     * @param response spot data from the simulator
     * @param sector associated sector (already synchronized)
     * @param mapSpotById existing spots indexed by externalSpotId
     * @return spot ready for persistence
     */
    private ParkingSpot buildOrUpdateSpot(SpotResponse response, GarageSector sector, Map<Integer, ParkingSpot> mapSpotById) {
        boolean occupied = Boolean.TRUE.equals(response.occupied());
        ParkingSpot existing = mapSpotById.get(response.id());
        if (existing != null) {
            existing.updateFrom(sector, response.lat(), response.lng(), occupied);
            return existing;
        }
        return new ParkingSpot(response.id(), sector, response.lat(), response.lng(), occupied);
    }

    /**
     * Sums the maximum capacity of all sectors.
     *
     * @param sectorsBySectorCode synchronized sectors
     * @return total garage capacity
     */
    private int calculateTotalCapacity(Map<String, GarageSector> sectorsBySectorCode) {
        return sectorsBySectorCode.values().stream()
                .mapToInt(GarageSector::getMaxCapacity)
                .sum();
    }

    /**
     * Updates the occupancy aggregate: removes old records and persists the new state.
     *
     * @param totalCapacity total garage capacity
     * @param occupiedCount number of occupied spots
     */
    private void synchronizeOccupancy(int totalCapacity, Long occupiedCount) {
        occupancyPort.deleteAll();

        GarageOccupancy occupancy = new GarageOccupancy(totalCapacity);
        occupancy.setOccupiedCount(occupiedCount.intValue());

        occupancyPort.save(occupancy);
        log.debug("[GARAGE] - [SYNC_DEBUG] occupancy={}/{}", occupiedCount, totalCapacity);
    }

    /**
     * Converts a string in "HH:mm" format to LocalTime.
     * Returns LocalTime.MIDNIGHT if null or blank.
     *
     * @param time string in "HH:mm" format (e.g. "08:00")
     * @return parsed time or midnight as fallback
     */
    private LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            log.warn("[GARAGE] - [SYNC_WARN] missing time value, defaulting to MIDNIGHT");
            return LocalTime.MIDNIGHT;
        }
        return LocalTime.parse(time, TIME_FORMATTER);
    }

}
