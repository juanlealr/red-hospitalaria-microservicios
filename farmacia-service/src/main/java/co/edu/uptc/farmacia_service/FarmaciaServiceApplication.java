package co.edu.uptc.farmacia_service;

import co.edu.uptc.farmacia_service.infraestructura.salida.aseguradora.AseguradoraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AseguradoraProperties.class)
public class FarmaciaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FarmaciaServiceApplication.class, args);
	}

}
