package co.edu.uptc.admisiones_service;

import co.edu.uptc.admisiones_service.grpc.LaboratorioGrpcGrpc.LaboratorioGrpcBlockingStub;
import co.edu.uptc.admisiones_service.infraestructura.salida.aseguradora.AseguradoraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ImportGrpcClients(target = "laboratorio", types = LaboratorioGrpcBlockingStub.class)
@EnableScheduling
@EnableConfigurationProperties(AseguradoraProperties.class)
public class AdmisionesServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(AdmisionesServiceApplication.class, args);
	}
}
