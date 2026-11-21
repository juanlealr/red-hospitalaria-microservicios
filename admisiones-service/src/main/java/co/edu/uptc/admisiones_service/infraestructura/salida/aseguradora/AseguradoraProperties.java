package co.edu.uptc.admisiones_service.infraestructura.salida.aseguradora;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admisiones.aseguradora")
public record AseguradoraProperties(String url, int connectTimeoutMs, int readTimeoutMs) {

    public AseguradoraProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:9095/api/aseguradora";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 2000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 3000;
        }
    }
}