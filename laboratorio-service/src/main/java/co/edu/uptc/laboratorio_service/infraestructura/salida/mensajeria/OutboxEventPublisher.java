package co.edu.uptc.laboratorio_service.infraestructura.salida.mensajeria;

import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventEntity;
import co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Publica en Kafka los eventos pendientes del outbox. Solo marca un evento como
 * publicado cuando el bróker confirma; si falla, corta el lote para preservar el
 * orden por orden de laboratorio (la clave de partición es el id de la orden).
 */
@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxEventPublisher(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${laboratorio.kafka.topic-ordenes:laboratorio.ordenes}") String topic) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${laboratorio.outbox.intervalo-ms:2000}")
    @Transactional
    public void publicarPendientes() {
        for (OutboxEventEntity evento : outboxRepository.buscarPendientesParaPublicar()) {
            try {
                kafkaTemplate.send(topic, evento.getClave(), evento.getPayload())
                        .get(5, TimeUnit.SECONDS);
                evento.marcarPublicado();
                log.info("Outbox: evento {} ({}) publicado en {}", evento.getId(), evento.getTipo(), topic);
            } catch (Exception e) {
                log.warn("Outbox: no se pudo publicar el evento {}. Se reintentará en el siguiente ciclo",
                        evento.getId());
                return;
            }
        }
    }
}
