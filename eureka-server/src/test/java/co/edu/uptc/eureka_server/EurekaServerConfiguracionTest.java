package co.edu.uptc.eureka_server;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EurekaServerConfiguracionTest {

    @Test
    void laAplicacionActivaElServidorEureka() {
        assertTrue(EurekaServerApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }

    @Test
    void escuchaEnElPuertoEsperadoYNoSeRegistraEnSiMismo() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/application.yaml")) {
            assertNotNull(in);
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("port: 8761"));
            assertTrue(yaml.contains("register-with-eureka: false"));
            assertTrue(yaml.contains("fetch-registry: false"));
        }
    }
}
