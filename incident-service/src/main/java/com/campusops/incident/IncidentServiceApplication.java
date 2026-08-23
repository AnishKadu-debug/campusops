package com.campusops.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class IncidentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncidentServiceApplication.class, args);
	}

}

