package co.edu.uptc.laboratorio_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaboratorioServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LaboratorioServiceApplication.class, args);
	}
}
