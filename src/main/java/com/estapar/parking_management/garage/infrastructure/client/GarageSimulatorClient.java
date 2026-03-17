package com.estapar.parking_management.garage.infrastructure.client;

import com.estapar.parking_management.garage.infrastructure.client.dto.GarageConfigurationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;

/**
 * Cliente HTTP para comunicação com o simulador de garagem.
 */
@Component
public class GarageSimulatorClient {

    private static final Logger log = LoggerFactory.getLogger(GarageSimulatorClient.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    private final RestClient restClient;

    public GarageSimulatorClient(
            @Value("${garage.simulator.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(converter);
                })
                .build();

        log.info("[GARAGE] - [CLIENT_INIT] baseUrl={}", baseUrl);
    }

    public GarageConfigurationResponse fetchGarageConfiguration() {
        log.debug("[GARAGE] - [FETCH] config");
        try {
            GarageConfigurationResponse response = restClient.get()
                    .uri("/garage")
                    .retrieve()
                    .body(GarageConfigurationResponse.class);

            if (response == null) {
                throw new GarageSimulatorException("Received null response from simulator");
            }

            log.debug("[GARAGE] - [FETCH_OK] sectors={}, spots={}",
                    response.garage() != null ? response.garage().size() : 0,
                    response.spots() != null ? response.spots().size() : 0);

            return response;
        } catch (RestClientException e) {
            log.error("[GARAGE] - [FETCH_FAIL] {}", e.getMessage());
            throw new GarageSimulatorException("Failed to fetch garage configuration", e);
        }
    }

    public static class GarageSimulatorException extends RuntimeException {
        public GarageSimulatorException(String message) {
            super(message);
        }

        public GarageSimulatorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
