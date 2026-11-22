package co.edu.uptc.auth_service.infraestructura.seguridad;

import co.edu.uptc.auth_service.dominio.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-pruebas-con-al-menos-32-bytes-1234567890";

    @Test
    void generaTokenFirmadoConHs256ConCorreoYRol() {
        JwtService jwt = new JwtService(SECRETO, 60_000);

        String token = jwt.generar("admin@hospital.com", Rol.ADMINISTRADOR);

        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        assertEquals("admin@hospital.com", claims.getSubject());
        assertEquals("ADMINISTRADOR", claims.get("rol", String.class));
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));

        String cabecera = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                StandardCharsets.UTF_8);
        assertTrue(cabecera.contains("HS256"), "Los demás servicios validan HS256");
    }

    @Test
    void expresaLaExpiracionEnSegundos() {
        assertEquals(120, new JwtService(SECRETO, 120_000).expiracionSegundos());
    }
}
