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
 * Serviço responsável pela sincronização do estado da garagem com o simulador externo.
 * Obtém a configuração (setores e vagas) do simulador, persiste ou atualiza os dados
 * localmente e mantém o agregado de ocupação (GarageOccupancy) atualizado.
 * Utiliza operações em lote (batch) para otimizar o número de round-trips ao banco.
 */
@Service
public class GarageSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(GarageSynchronizationService.class);
    /** Formato esperado para horários (ex.: "08:00", "22:00"). */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final GarageSimulatorClient simulatorClient;
    private final GarageSectorPort sectorPort;
    private final ParkingSpotPort spotPort;
    private final GarageOccupancyPort occupancyPort;

    /**
     * Constrói o serviço de sincronização com as dependências necessárias.
     *
     * @param simulatorClient cliente para obter a configuração do simulador
     * @param sectorPort     port de persistência de setores
     * @param spotPort       port de persistência de vagas
     * @param occupancyPort  port de persistência do agregado de ocupação
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
     * Sincroniza o estado completo da garagem com o simulador.
     * Fluxo: (1) busca configuração do simulador, (2) sincroniza setores em lote,
     * (3) sincroniza vagas em lote, (4) atualiza o agregado de ocupação.
     *
     * @throws IllegalArgumentException se a configuração não contiver ao menos um setor
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
     * Sincroniza os setores: carrega existentes em lote, atualiza ou cria, persiste em lote.
     *
     * @param sectors lista de setores retornados pelo simulador
     * @return mapa código do setor → setor persistido
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
     * Busca setores existentes pelos códigos e monta mapa.
     *
     * @param sectorCodes códigos dos setores a buscar
     * @return mapa código → setor (vazio para setores inexistentes)
     */
    private Map<String, GarageSector> findAndcreateMapSectorsByCode(List<String> sectorCodes) {
        return sectorPort.findAllBySectorCodeIn(sectorCodes)
                .stream()
                .collect(Collectors.toMap(GarageSector::getSectorCode, Function.identity()));
    }

    /**
     * Monta a lista de setores para persistência: atualiza existentes ou cria novos.
     *
     * @param sectors respostas do simulador
     * @param mapSectorByCode setores já existentes no banco, indexados por código
     * @return lista pronta para saveAll
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
     * Sincroniza as vagas: filtra inválidas, carrega existentes em lote, atualiza ou cria, persiste em lote.
     *
     * @param spots vagas retornadas pelo simulador
     * @param sectorsBySectorCode  setores já sincronizados (necessário para referência)
     * @return quantidade de vagas ocupadas após a sincronização
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
     * Filtra vagas que referenciam setores inexistentes (são ignoradas com log de aviso).
     *
     * @param spots vagas a validar
     * @param sectorsBySectorCode  setores conhecidos
     * @return apenas vagas cujo setor existe
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
     * Busca vagas existentes pelos IDs externos e monta mapa para lookup.
     *
     * @param spotIds IDs externos das vagas (do simulador)
     * @return mapa externalSpotId → vaga
     */
    private Map<Integer, ParkingSpot> findAndcreateMapSpotsById(List<Integer> spotIds) {
        return spotPort.findAllByExternalSpotIdIn(spotIds).stream()
                .collect(Collectors.toMap(ParkingSpot::getExternalSpotId, Function.identity()));
    }

    /**
     * Cria nova vaga ou atualiza existente com dados do simulador.
     *
     * @param response dados da vaga vindos do simulador
     * @param sector setor associado (já sincronizado)
     * @param mapSpotById vagas existentes indexadas por externalSpotId
     * @return vaga pronta para persistência
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
     * Soma a capacidade máxima de todos os setores.
     *
     * @param sectorsBySectorCode setores sincronizados
     * @return capacidade total da garagem
     */
    private int calculateTotalCapacity(Map<String, GarageSector> sectorsBySectorCode) {
        return sectorsBySectorCode.values().stream()
                .mapToInt(GarageSector::getMaxCapacity)
                .sum();
    }

    /**
     * Atualiza o agregado de ocupação: remove registros antigos e persiste o novo estado.
     *
     * @param totalCapacity capacidade total da garagem
     * @param occupiedCount quantidade de vagas ocupadas
     */
    private void synchronizeOccupancy(int totalCapacity, Long occupiedCount) {
        occupancyPort.deleteAll();

        GarageOccupancy occupancy = new GarageOccupancy(totalCapacity);
        occupancy.setOccupiedCount(occupiedCount.intValue());

        occupancyPort.save(occupancy);
        log.debug("[GARAGE] - [SYNC_DEBUG] occupancy={}/{}", occupiedCount, totalCapacity);
    }

    /**
     * Converte string no formato "HH:mm" para LocalTime.
     * Retorna LocalTime.MIDNIGHT se nulo ou em branco.
     *
     * @param time string no formato "HH:mm" (ex.: "08:00")
     * @return horário parseado ou meia-noite como fallback
     */
    private LocalTime parseTime(String time) {
        if (time == null || time.isBlank()) {
            log.warn("[GARAGE] - [SYNC_WARN] missing time value, defaulting to MIDNIGHT");
            return LocalTime.MIDNIGHT;
        }
        return LocalTime.parse(time, TIME_FORMATTER);
    }

}
