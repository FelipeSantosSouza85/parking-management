package com.estapar.parking_management;

import com.estapar.parking_management.garage.application.port.GarageOccupancyPort;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.GarageSectorJpaRepository;
import com.estapar.parking_management.garage.infrastructure.persistence.repository.ParkingSpotJpaRepository;
import com.estapar.parking_management.parking.infrastructure.persistence.repository.ParkingSessionJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	@SuppressWarnings("resource")
	MySQLContainer<?> mysqlContainer() {
		return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
				.withDatabaseName("parking_management_test");
	}

	@Bean
	@ConditionalOnBean(GarageOccupancyPort.class)
	TestDataCleaner testDataCleaner(
			ParkingSessionJpaRepository parkingSessionJpaRepository,
			ParkingSpotJpaRepository parkingSpotJpaRepository,
			GarageSectorJpaRepository garageSectorJpaRepository,
			GarageOccupancyPort garageOccupancyPort
	) {
		return new TestDataCleaner(
				parkingSessionJpaRepository,
				parkingSpotJpaRepository,
				garageSectorJpaRepository,
				garageOccupancyPort
		);
	}

}
