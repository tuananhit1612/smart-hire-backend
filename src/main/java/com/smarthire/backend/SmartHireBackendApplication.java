package com.smarthire.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartHireBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartHireBackendApplication.class, args);
	}

}
