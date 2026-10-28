package co.edu.uptc.farmacia_service.infraestructura.salida.aseguradora;

import co.edu.uptc.farmacia_service.aplicacion.ConsultaCoberturaExterna;
import co.edu.uptc.farmacia_service.compartido.ServicioExternoNoDisponibleException;
import co.edu.uptc.farmacia_service.dominio.Cobertura;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Adaptador secundario real del puerto ConsultaCoberturaExterna: llama por
 * REST a la aseguradora externa.
 *
 * Aplica el patrón de la Lección 6 para no bloquear la operación clínica:
 * timeouts acotados y Circuit Breaker (Resilience4j) — si el externo falla
 * repetidamente, las llamadas siguientes fallan de inmediato con 503 en vez
 * de agotar hilos esperando.
 *
 * Se activa con el perfil "aseguradora-rest".
 */
@Component
@Profile("aseguradora-rest")
public class CoberturaAseguradoraRestAdapter implements ConsultaCoberturaExterna {

    private static final Logger log = LoggerFactory.getLogger(CoberturaAseguradoraRestAdapter.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public CoberturaAseguradoraRestAdapter(AseguradoraProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(requestFactory)
                .build();

        this.circuitBreaker = CircuitBreaker.of("aseguradora",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build());
        this.circuitBreaker.getEventPublisher().onStateTransition(
                e -> log.warn("Circuit Breaker aseguradora: {}", e.getStateTransition()));
    }

    @Override
    public Cobertura consultar(String pacienteId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                CoberturaRespuesta respuesta = restClient.get()
                        .uri("/cobertura/{pacienteId}", pacienteId)
                        .retrieve()
                        .body(CoberturaRespuesta.class);

                if (respuesta == null || respuesta.porcentaje() == null) {
                    throw new ServicioExternoNoDisponibleException(
                            "La aseguradora respondió sin porcentaje de cobertura", null);
                }
                return new Cobertura(respuesta.porcentaje());
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit Breaker ABIERTO: aseguradora no disponible");
            throw new ServicioExternoNoDisponibleException(
                    "La aseguradora no está disponible (circuito abierto)", e);
        } catch (RestClientException e) {
            throw new ServicioExternoNoDisponibleException(
                    "No se pudo consultar la cobertura en la aseguradora", e);
        }
    }
}
