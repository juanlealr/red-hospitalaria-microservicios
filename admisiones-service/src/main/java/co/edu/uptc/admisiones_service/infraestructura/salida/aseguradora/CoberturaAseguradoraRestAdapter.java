package co.edu.uptc.admisiones_service.infraestructura.salida.aseguradora;

import co.edu.uptc.admisiones_service.aplicacion.ConsultaCoberturaExterna;
import co.edu.uptc.admisiones_service.compartido.ServicioNoDisponibleException;
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

        this.circuitBreaker = CircuitBreaker.of("aseguradora-admisiones",
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
    public int consultarPorcentajeCobertura(String pacienteId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                CoberturaRespuesta respuesta = restClient.get()
                        .uri("/cobertura/{pacienteId}", pacienteId)
                        .retrieve()
                        .body(CoberturaRespuesta.class);

                if (respuesta == null || respuesta.porcentaje() == null) {
                    throw new ServicioNoDisponibleException(
                            "La aseguradora respondió sin porcentaje de cobertura.", null);
                }
                return respuesta.porcentaje();
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit Breaker ABIERTO: aseguradora no disponible");
            throw new ServicioNoDisponibleException(
                    "La aseguradora externa no está disponible (Circuito Abierto).", e);
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar la cobertura en la aseguradora externa.", e);
        }
    }
}
