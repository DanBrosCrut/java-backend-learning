package com.example.moysklad.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MoyskladApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoyskladApiApplication.class, args);
	}

}
