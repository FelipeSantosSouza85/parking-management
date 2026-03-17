package com.estapar.parking_management;

import org.springframework.boot.SpringApplication;

public class TestParkingManagementApplication {

	public static void main(String[] args) {
		SpringApplication.from(ParkingManagementApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
