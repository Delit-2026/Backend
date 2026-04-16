package com.dealit.dealit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DealitApplication {

	public static void main(String[] args) {
		SpringApplication.run(DealitApplication.class, args);
	}

}
