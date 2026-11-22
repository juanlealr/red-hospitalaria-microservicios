package co.edu.uptc.admisiones_service.infraestructura.seguridad;

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
                        .requestMatchers(HttpMethod.POST, "/api/admisiones/ingreso").hasAuthority("ROLE_ADMISIONES")
                        .requestMatchers(HttpMethod.POST, "/api/admisiones/*/verificar-cobertura")
                        .hasAnyAuthority("ROLE_ADMISIONES", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/admisiones/cobertura/*")
                        .hasAnyAuthority("ROLE_ADMISIONES", "ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/admisiones/*/traslado")
                        .hasAnyAuthority("ROLE_MEDICO")
                        .requestMatchers(HttpMethod.PUT, "/api/admisiones/*/alta")
                        .hasAnyAuthority("ROLE_MEDICO")
                        .requestMatchers(HttpMethod.POST, "/api/admisiones/inventario-camas")
                        .hasAuthority("ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/admisiones/inventario-camas/reporte-ocupacion")
                        .hasAuthority("ROLE_ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/admisiones/**")
                        .hasAnyAuthority("ROLE_ADMISIONES", "ROLE_MEDICO", "ROLE_ADMINISTRADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(manejador)
                        .accessDeniedHandler(manejador)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }
}
