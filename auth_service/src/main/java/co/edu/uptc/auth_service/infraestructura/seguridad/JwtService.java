// infraestructura/seguridad/JwtService.java
package co.edu.uptc.auth_service.infraestructura.seguridad;

import co.edu.uptc.auth_service.dominio.Rol;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expiracionMs;

    public JwtService(
            @Value("${seguridad.jwt.secret}") String secreto,
            @Value("${seguridad.jwt.expiracion-ms:7200000}") long expiracionMs) {
        this.key = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generar(String email, Rol rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(expiracionMs)))
                .signWith(key)
                .compact();
    }

    public long expiracionSegundos() {
        return expiracionMs / 1000;
    }
}