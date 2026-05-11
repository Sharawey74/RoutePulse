package com.routepulse.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * RoutePulse — AI-Based Delivery Optimizer.
 * Entry point for the Spring Boot application.
 * scanBasePackages ensures all com.routepulse sub-packages are scanned:
 * algorithm, simulation, state, api, domain, config, reporting.
 */
@SpringBootApplication(scanBasePackages = "com.routepulse")
@ConfigurationPropertiesScan("com.routepulse")
public class RoutepulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoutepulseApplication.class, args);
	}

}
