package co.edu.uptc.admisiones_service.infraestructura.salida.persistencia;

import jakarta.persistence.*;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clave; // ID del paciente o admisión (partición de Kafka)
    private String tipo; // "PacienteDadoDeAlta"

    @Column(columnDefinition = "TEXT")
    private String payload; // JSON con la información del evento

    private boolean publicado;

    protected OutboxEvent() {
    }

    public OutboxEvent(String clave, String tipo, String payload) {
        this.clave = clave;
        this.tipo = tipo;
        this.payload = payload;
        this.publicado = false;
    }

    public Long getId() {
        return id;
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

    public boolean isPublicado() {
        return publicado;
    }

    public void marcarPublicado() {
        this.publicado = true;
    }
}
