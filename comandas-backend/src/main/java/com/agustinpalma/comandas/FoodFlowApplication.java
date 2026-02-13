package com.agustinpalma.comandas;

import com.agustinpalma.comandas.infrastructure.config.MeisenProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MeisenProperties.class)
public class FoodFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodFlowApplication.class, args);
	}

}
