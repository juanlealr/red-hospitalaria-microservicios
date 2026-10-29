package co.edu.uptc.farmacia_service.infraestructura.salida.aseguradora;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del sistema externo de la aseguradora.
 *
 * @param url                base de la API externa
 * @param connectTimeoutMs   timeout de conexión
 * @param readTimeoutMs      timeout de lectura
 */
@ConfigurationProperties(prefix = "farmacia.aseguradora")
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
