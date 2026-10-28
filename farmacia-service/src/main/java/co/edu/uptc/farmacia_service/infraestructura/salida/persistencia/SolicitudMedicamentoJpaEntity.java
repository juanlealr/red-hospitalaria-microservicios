package co.edu.uptc.farmacia_service.infraestructura.salida.persistencia;

import co.edu.uptc.farmacia_service.dominio.EstadoSolicitudMedicamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Modelo de persistencia del Agregado. Vive en infraestructura para que el
 * paquete dominio no conozca jakarta.persistence: el núcleo se mantiene
 * limpio y sustituible.
 */
@Entity
@Table(name = "solicitudes_medicamento")
public class SolicitudMedicamentoJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "paciente_id", nullable = false)
    private String pacienteId;

    @Column(name = "medico_id", nullable = false)
    private String medicoId;

    @Column(name = "medicamento", nullable = false)
    private String medicamento;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 50)
    private EstadoSolicitudMedicamento estado;

    @Column(name = "cobertura_porcentaje")
    private Integer coberturaPorcentaje;

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    protected SolicitudMedicamentoJpaEntity() {
        // exigido por JPA
    }

    public SolicitudMedicamentoJpaEntity(
            String id,
            String pacienteId,
            String medicoId,
            String medicamento,
            LocalDateTime fechaSolicitud,
            EstadoSolicitudMedicamento estado,
            Integer coberturaPorcentaje,
            LocalDateTime fechaDespacho) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.medicamento = medicamento;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = estado;
        this.coberturaPorcentaje = coberturaPorcentaje;
        this.fechaDespacho = fechaDespacho;
    }

    public String getId() {
        return id;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public String getMedicoId() {
        return medicoId;
    }

    public String getMedicamento() {
        return medicamento;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoSolicitudMedicamento getEstado() {
        return estado;
    }

    public Integer getCoberturaPorcentaje() {
        return coberturaPorcentaje;
    }

    public LocalDateTime getFechaDespacho() {
        return fechaDespacho;
    }
}
