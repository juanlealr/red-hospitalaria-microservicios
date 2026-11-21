package co.edu.uptc.gateway.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${seguridad.jwt.secret}")
    private String secreto;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKeySpec key = new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            return rol == null ? Flux.empty() : Flux.just(new SimpleGrantedAuthority("ROLE_" + rol));
        });
        return converter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
            ManejadorErroresSeguridad manejador) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(manejador)
                        .accessDeniedHandler(manejador))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/auth/usuarios").hasAuthority("ROLE_ADMINISTRADOR")
                        .pathMatchers(HttpMethod.POST, "/api/admisiones/ingreso").hasAuthority("ROLE_ADMISIONES")
                        .pathMatchers(HttpMethod.POST, "/api/admisiones/*/verificar-cobertura")
                        .hasAuthority("ROLE_ADMISIONES")
                        .pathMatchers(HttpMethod.GET, "/api/admisiones/cobertura/*")
                        .hasAnyAuthority("ROLE_ADMISIONES", "ROLE_ADMINISTRADOR")
                        .pathMatchers(HttpMethod.PUT, "/api/admisiones/*/traslado").hasAuthority("ROLE_MEDICO")
                        .pathMatchers(HttpMethod.PUT, "/api/admisiones/*/alta").hasAuthority("ROLE_MEDICO")
                        .pathMatchers(HttpMethod.POST, "/api/admisiones/inventario-camas")
                        .hasAnyAuthority("ROLE_ADMINISTRADOR", "ROLE_ADMISIONES")
                        .pathMatchers(HttpMethod.GET, "/api/admisiones/inventario-camas/reporte-ocupacion")
                        .hasAuthority("ROLE_ADMINISTRADOR")
                        .pathMatchers(HttpMethod.GET, "/api/admisiones/**")
                        .hasAnyAuthority("ROLE_ADMISIONES", "ROLE_MEDICO", "ROLE_ADMINISTRADOR")
                        .pathMatchers(HttpMethod.GET, "/api/consultas/admisiones/reporte/**")
                        .hasAuthority("ROLE_ADMINISTRADOR")
                        .pathMatchers("/api/consultas/admisiones/**")
                        .hasAnyAuthority("ROLE_MEDICO", "ROLE_ADMISIONES", "ROLE_ADMINISTRADOR")
                        .pathMatchers(HttpMethod.POST, "/api/farmacia/solicitudes")
                        .hasAnyAuthority("ROLE_MEDICO", "ROLE_FARMACEUTICO")
                        .pathMatchers(HttpMethod.POST, "/api/farmacia/solicitudes/*/despachar")
                        .hasAuthority("ROLE_FARMACEUTICO")
                        .pathMatchers("/api/farmacia/**")
                        .hasAnyAuthority("ROLE_FARMACEUTICO", "ROLE_ADMISIONES", "ROLE_MEDICO")
                        .pathMatchers("/api/laboratorio/**").hasAnyAuthority("ROLE_MEDICO", "ROLE_LABORATORIO")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(manejador)
                        .accessDeniedHandler(manejador)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }
}
