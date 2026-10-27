package co.edu.uptc.admisiones_service.infraestructura.salida.laboratorio;

import co.edu.uptc.admisiones_service.compartido.ServicioNoDisponibleException;
import co.edu.uptc.admisiones_service.grpc.LaboratorioGrpcGrpc.LaboratorioGrpcBlockingStub;
import co.edu.uptc.admisiones_service.grpc.OrdenesPendientesRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class VerificadorLaboratorioGrpcAdapter {

    private static final Logger log = LoggerFactory.getLogger(VerificadorLaboratorioGrpcAdapter.class);
    private final LaboratorioGrpcBlockingStub stub;
    private final CircuitBreaker circuitBreaker;

    public VerificadorLaboratorioGrpcAdapter(LaboratorioGrpcBlockingStub stub) {
        this.stub = stub;
        this.circuitBreaker = CircuitBreaker.of("laboratorio-service",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build());
        this.circuitBreaker.getEventPublisher().onStateTransition(
                e -> log.warn("Circuit Breaker laboratorio-service: {}", e.getStateTransition()));
    }

    public boolean tieneOrdenesPendientes(String pacienteId) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                var request = OrdenesPendientesRequest.newBuilder()
                        .setPacienteId(pacienteId)
                        .build();
                return stub.withDeadlineAfter(5, TimeUnit.SECONDS)
                        .verificarOrdenesPendientes(request)
                        .getTienePendientes();
            });
        } catch (CallNotPermittedException e) {
            log.warn("Circuit Breaker ABIERTO: laboratorio-service no disponible");
            throw new ServicioNoDisponibleException(
                    "El servicio de laboratorio no está disponible (Circuito Abierto)", e);
        } catch (StatusRuntimeException e) {
            throw new ServicioNoDisponibleException(
                    "No se pudo contactar al servicio de laboratorio via gRPC: " + e.getStatus().getCode(), e);
        }
    }
}
