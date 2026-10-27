package co.edu.uptc.admisiones_service.infraestructura.salida.mensajeria;

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

    @Scheduled(fixedDelay = 2000) // Revisa cada 2 segundos
    @Transactional
    public void publicarPendientes() {
        for (var evento : outboxRepository.findByPublicadoFalseOrderByIdAsc()) {
            try {
                // .get(5, SECONDS) fuerza la confirmación síncrona del bróker
                kafkaTemplate.send("admisiones.paciente-dado-alta", evento.getClave(), evento.getPayload())
                        .get(5, TimeUnit.SECONDS);

                evento.marcarPublicado();
                log.info("Outbox: Evento {} ({}) publicado en Kafka", evento.getId(), evento.getTipo());
            } catch (Exception e) {
                log.warn("Outbox: No se pudo enviar el evento {}. Se reintentará en el siguiente ciclo",
                        evento.getId());
                return; // Se detiene el bucle para preservar el orden estricto de eventos
            }
        }
    }
}