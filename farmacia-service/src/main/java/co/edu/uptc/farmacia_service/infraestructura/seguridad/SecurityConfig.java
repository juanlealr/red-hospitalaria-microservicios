package co.edu.uptc.farmacia_service.infraestructura.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Resource server de farmacia: valida el JWT en cada petición y aplica los
 * mismos roles que el gateway.
 *
 * Esto cierra el caso que el ATAM-LITE marcaba como riesgo 2: sin esta clase,
 * quien llamara directo al puerto 8081 saltándose el gateway leía y escribía
 * solicitudes sin token. La lógica de negocio no cambia; solo deja de confiar
 * en que la única barrera sea el gateway.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${seguridad.jwt.secret}")
    private String secreto;

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            return rol == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ManejadorErroresSeguridad manejador)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(manejador)
                        .accessDeniedHandler(manejador))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/status").permitAll()
                        // Mismos roles que el gateway (/api/farmacia/** -> FARMACEUTICO, ADMISIONES, MEDICO).
                        // El gateway es la primera barrera; aquí se afina por acción (defensa en profundidad).
                        // HU-06: la solicitud de medicamento la crea el médico o el farmacéutico.
                        .requestMatchers(HttpMethod.POST, "/api/farmacia/solicitudes")
                        .hasAnyAuthority("ROLE_MEDICO", "ROLE_FARMACEUTICO")
                        // HU-06: solo el farmacéutico despacha.
                        .requestMatchers(HttpMethod.POST, "/api/farmacia/solicitudes/*/despachar")
                        .hasAuthority("ROLE_FARMACEUTICO")
                        // HU-07: verificar cobertura forma parte del despacho; mismo criterio.
                        .requestMatchers(HttpMethod.POST, "/api/farmacia/solicitudes/*/verificar-cobertura")
                        .hasAnyAuthority("ROLE_MEDICO", "ROLE_FARMACEUTICO")
                        // Consultas: los tres roles que el gateway deja pasar a farmacia.
                        .requestMatchers(HttpMethod.GET, "/api/farmacia/**")
                        .hasAnyAuthority("ROLE_FARMACEUTICO", "ROLE_ADMISIONES", "ROLE_MEDICO")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(manejador)
                        .accessDeniedHandler(manejador)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }
}
