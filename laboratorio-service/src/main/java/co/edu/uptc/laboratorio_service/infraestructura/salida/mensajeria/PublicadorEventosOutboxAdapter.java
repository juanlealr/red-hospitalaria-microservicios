package co.edu.uptc.laboratorio_service.infraestructura.salida.mensajeria;

import co.edu.uptc.laboratorio_service.aplicacion.PublicadorEventosOrden;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventEntity;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convierte el hecho de dominio en una fila de outbox dentro de la transacción en
 * curso. La entrega real a Kafka la hace OutboxEventPublisher.
 */
@Component
public class PublicadorEventosOutboxAdapter implements PublicadorEventosOrden {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PublicadorEventosOutboxAdapter(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(String tipoEvento, OrdenLaboratorio orden) {
        String eventoId = UUID.randomUUID().toString();
        LocalDateTime ocurridoEn = LocalDateTime.now();
        String ordenId = orden.getId().toString();

        EventoOrdenPayload payload = new EventoOrdenPayload(
                eventoId,
                tipoEvento,
                ocurridoEn.toString(),
                ordenId,
                orden.getPacienteId(),
                orden.getTipoExamen(),
                orden.getEstado().name(),
                orden.tieneResultado() ? orden.getResultado().valor() : null,
                orden.tieneResultado() ? orden.getResultado().unidad() : null,
                orden.getFechaSolicitud().toString());

        outboxRepository.save(new OutboxEventEntity(eventoId, ordenId, tipoEvento, serializar(payload), ocurridoEn));
    }

    private String serializar(EventoOrdenPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            // UncheckedIOException (no IllegalStateException): el GlobalExceptionHandler mapea
            // IllegalStateException a 409, y un fallo de serialización debe ser un 500.
            throw new UncheckedIOException("No se pudo serializar el evento " + payload.tipo(), e);
        }
    }
}
