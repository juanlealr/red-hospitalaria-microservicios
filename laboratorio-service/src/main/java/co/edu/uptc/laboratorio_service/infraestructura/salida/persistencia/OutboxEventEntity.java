package co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Fila del Transactional Outbox (misma forma que en farmacia-service). */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true, length = 36)
    private String eventoId;

    @Column(name = "clave", nullable = false)
    private String clave;

    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(name = "publicado", nullable = false)
    private boolean publicado;

    protected OutboxEventEntity() {
        // exigido por JPA
    }

    public OutboxEventEntity(String eventoId, String clave, String tipo, String payload, LocalDateTime ocurridoEn) {
        this.eventoId = eventoId;
        this.clave = clave;
        this.tipo = tipo;
        this.payload = payload;
        this.ocurridoEn = ocurridoEn;
        this.publicado = false;
    }

    public void marcarPublicado() {
        this.publicado = true;
    }

    public Long getId() {
        return id;
    }

    public String getEventoId() {
        return eventoId;
    }

    public String getClave() {
        return clave;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public boolean isPublicado() {
        return publicado;
    }
}
