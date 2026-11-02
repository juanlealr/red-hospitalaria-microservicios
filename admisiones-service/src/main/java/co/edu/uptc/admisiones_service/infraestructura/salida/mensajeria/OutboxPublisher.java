package co.edu.uptc.admisiones_service.infraestructura.salida.mensajeria;

import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEvent;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publicarPendientes() {
        for (OutboxEvent evento : outboxRepository.findByPublicadoFalseOrderByIdAsc()) {
            try {
                String topico = determinarTopico(evento.getTipo());
                kafkaTemplate.send(topico, evento.getClave(), evento.getPayload())
                        .get(5, TimeUnit.SECONDS);

                evento.marcarPublicado();
                log.info("Outbox: Evento {} ({}) publicado en tópico '{}'", evento.getId(), evento.getTipo(), topico);
            } catch (Exception e) {
                log.warn("Outbox: No se pudo enviar el evento {} (se reintentará): {}", evento.getId(), e.getMessage());
                return; // Mantiene orden estricto interrumpiendo el lote
            }
        }
    }

    private String determinarTopico(String tipoEvento) {
        return switch (tipoEvento) {
            case "PacienteIngresado" -> "admisiones.ingresos-registrados";
            case "PacienteTrasladado" -> "admisiones.pacientes-trasladados";
            case "PacienteDadoDeAlta" -> "admisiones.paciente-dado-alta";
            default -> "admisiones.eventos-generales";
        };
    }
}