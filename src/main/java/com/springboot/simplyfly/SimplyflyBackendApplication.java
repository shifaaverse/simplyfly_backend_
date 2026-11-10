package com.springboot.simplyfly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // used to release seats held for unpaid bookings
// SonarQube automatic analysis test
public class SimplyflyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimplyflyBackendApplication.class, args);
	}

}