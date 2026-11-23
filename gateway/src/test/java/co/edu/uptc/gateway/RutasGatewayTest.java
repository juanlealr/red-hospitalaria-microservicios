package co.edu.uptc.gateway;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica la configuración de enrutamiento sin levantar el contexto de Spring. */
class RutasGatewayTest {

    private String configuracion() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/application.yaml")) {
            assertNotNull(in, "application.yaml debe estar en el classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void enrutaCadaDominioASuServicioPorEureka() throws IOException {
        String yaml = configuracion();

        assertTrue(yaml.contains("lb://auth-service") && yaml.contains("/api/auth/**"));
        assertTrue(yaml.contains("lb://admisiones-service") && yaml.contains("/api/admisiones/**"));
        assertTrue(yaml.contains("lb://farmacia-service") && yaml.contains("/api/farmacia/**"));
        assertTrue(yaml.contains("lb://laboratorio-service") && yaml.contains("/api/laboratorio/**"));
        assertTrue(yaml.contains("lb://consultas-admisiones-service")
                && yaml.contains("/api/consultas/admisiones/**"));
    }

    @Test
    void elSecretoJwtSeLeeDeLaVariableDeEntorno() throws IOException {
        String yaml = configuracion();

        assertTrue(yaml.contains("${JWT_SECRET"),
                "El secreto debe leerse de JWT_SECRET, no estar escrito en el archivo");
        assertFalse(yaml.matches("(?s).*secret:\\s*[A-Za-z0-9+/]{20,}.*"),
                "No debe haber un secreto literal en application.yaml: va en el entorno (.env)");
    }
}
