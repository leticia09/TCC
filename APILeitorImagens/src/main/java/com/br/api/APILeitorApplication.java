package com.br.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.AbstractEnvironment;

@SpringBootApplication
public class APILeitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(APILeitorApplication.class, args);
		System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "prod");
		System.setProperty("java.awt.headless", "false");
	}

}
