package co.edu.uptc.farmacia_service.infraestructura.salida.mensajeria;

import co.edu.uptc.farmacia_service.aplicacion.PublicadorEventosSolicitud;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import co.edu.uptc.farmacia_service.infraestructura.salida.persistencia.OutboxEventEntity;
import co.edu.uptc.farmacia_service.infraestructura.salida.persistencia.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adaptador secundario del puerto de publicación: convierte el hecho de
 * dominio en una fila de outbox dentro de la transacción en curso. La
 * entrega real a Kafka la hace OutboxEventPublisher.
 */
@Component
public class PublicadorEventosOutboxAdapter implements PublicadorEventosSolicitud {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PublicadorEventosOutboxAdapter(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(String tipoEvento, SolicitudMedicamento solicitud) {
        String eventoId = UUID.randomUUID().toString();
        LocalDateTime ocurridoEn = LocalDateTime.now();

        EventoSolicitudPayload payload = new EventoSolicitudPayload(
                eventoId,
                tipoEvento,
                ocurridoEn,
                solicitud.id(),
                solicitud.pacienteId(),
                solicitud.medicoId(),
                solicitud.medicamento(),
                solicitud.estado().name(),
                solicitud.cobertura() == null ? null : solicitud.cobertura().porcentaje(),
                solicitud.fechaSolicitud(),
                solicitud.fechaDespacho());

        outboxRepository.save(new OutboxEventEntity(
                eventoId,
                solicitud.id(),
                tipoEvento,
                serializar(payload),
                ocurridoEn));
    }

    private String serializar(EventoSolicitudPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + payload.tipo(), e);
        }
    }
}
