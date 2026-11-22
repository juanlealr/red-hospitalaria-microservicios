package co.edu.uptc.laboratorio_service.infraestructura.salida.mensajeria;

import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventEntity;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * El publicador solo debe marcar un evento como publicado cuando Kafka confirma, y ante un
 * fallo debe cortar el lote (para no desordenar eventos de una misma orden) y reintentar luego.
 */
class OutboxEventPublisherTest {

    private static final String TOPIC = "laboratorio.ordenes";

    private final List<OutboxEventEntity> filas = new ArrayList<>();
    private KafkaTemplate<String, String> kafka;
    private OutboxEventPublisher publicador;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        kafka = mock(KafkaTemplate.class);
        publicador = new OutboxEventPublisher(repositorioConPendientes(), kafka, TOPIC);
    }

    @Test
    void publicaLosPendientesEnElTopicConLaClaveYElPayloadYLosMarca() {
        OutboxEventEntity a = fila("orden-1", "ExamenSolicitado", "{\"n\":1}");
        OutboxEventEntity b = fila("orden-1", "ResultadoRegistrado", "{\"n\":2}");
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publicador.publicarPendientes();

        verify(kafka).send(TOPIC, "orden-1", "{\"n\":1}");
        verify(kafka).send(TOPIC, "orden-1", "{\"n\":2}");
        assertTrue(a.isPublicado());
        assertTrue(b.isPublicado());
    }

    @Test
    void sinPendientesNoHablaConKafka() {
        publicador.publicarPendientes();

        verifyNoInteractions(kafka);
    }

    @Test
    void siKafkaFallaEnElPrimeroNoMarcaNadaYNoIntentaElSiguiente() {
        OutboxEventEntity a = fila("orden-1", "ExamenSolicitado", "{\"n\":1}");
        OutboxEventEntity b = fila("orden-1", "ResultadoRegistrado", "{\"n\":2}");
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caído")));

        publicador.publicarPendientes();

        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        assertFalse(a.isPublicado());
        assertFalse(b.isPublicado());
    }

    @Test
    void siKafkaFallaEnElSegundoElPrimeroQuedaPublicadoYElSegundoPendiente() {
        OutboxEventEntity a = fila("orden-1", "ExamenSolicitado", "{\"n\":1}");
        OutboxEventEntity b = fila("orden-2", "ExamenSolicitado", "{\"n\":2}");
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        when(kafka.send(eq(TOPIC), eq("orden-2"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caído")));

        publicador.publicarPendientes();

        assertTrue(a.isPublicado());
        assertFalse(b.isPublicado());
    }

    @Test
    void enElSiguienteCicloReintentaLosQueQuedaronPendientes() {
        OutboxEventEntity a = fila("orden-1", "ExamenSolicitado", "{\"n\":1}");
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker caído")));
        publicador.publicarPendientes();
        assertFalse(a.isPublicado());

        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        publicador.publicarPendientes();

        assertTrue(a.isPublicado());
    }

    @Test
    void unEventoYaPublicadoNoSeVuelveAEnviar() {
        OutboxEventEntity a = fila("orden-1", "ExamenSolicitado", "{\"n\":1}");
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        publicador.publicarPendientes();
        publicador.publicarPendientes();

        verify(kafka, times(1)).send(anyString(), anyString(), anyString());
        assertTrue(a.isPublicado());
    }

    private OutboxEventEntity fila(String clave, String tipo, String payload) {
        OutboxEventEntity fila = new OutboxEventEntity(
                java.util.UUID.randomUUID().toString(), clave, tipo, payload, LocalDateTime.now());
        filas.add(fila);
        return fila;
    }

    /** Repositorio de mentira: devuelve las filas aún no publicadas, como la consulta nativa real. */
    private OutboxEventRepository repositorioConPendientes() {
        return (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(),
                new Class<?>[]{OutboxEventRepository.class},
                (proxy, metodo, args) -> {
                    if (metodo.getDeclaringClass() == Object.class) {
                        return switch (metodo.getName()) {
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> "repositorioOutboxFalso";
                        };
                    }
                    if (metodo.getName().equals("buscarPendientesParaPublicar")) {
                        return filas.stream().filter(f -> !f.isPublicado()).toList();
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
    }
}
