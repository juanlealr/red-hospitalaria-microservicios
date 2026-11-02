package co.edu.uptc.admisiones_service.dominio;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "admisiones")
public class Admision {

    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("pacienteId")
    private String pacienteId;

    @JsonProperty("sedeActualId")
    private String sedeActualId;

    @Enumerated(EnumType.STRING)
    @JsonProperty("estado")
    private EstadoAdmision estado;

    @JsonProperty("fechaIngreso")
    private LocalDateTime fechaIngreso;

    @JsonProperty("fechaAlta")
    private LocalDateTime fechaAlta;

    @ElementCollection
    @CollectionTable(name = "admision_traslados", joinColumns = @JoinColumn(name = "admision_id"))
    @JsonProperty("historialTraslados")
    private List<Traslado> historialTraslados = new ArrayList<>();

    protected Admision() {
        // Exigido por Hibernate
    }

    public Admision(String id, String pacienteId, String sedeInicialId, LocalDateTime fechaIngreso) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.sedeActualId = sedeInicialId;
        this.estado = EstadoAdmision.ACTIVA;
        this.fechaIngreso = fechaIngreso;
    }

    public void registrarTraslado(String sedeDestinoId, LocalDateTime fecha) {
        if (estado != EstadoAdmision.ACTIVA) {
            throw new IllegalStateException("Solo se puede trasladar una admisión activa.");
        }
        historialTraslados.add(new Traslado(this.sedeActualId, sedeDestinoId, fecha));
        this.sedeActualId = sedeDestinoId;
    }

    public void darDeAlta(boolean tieneOrdenesLaboratorioPendientes, LocalDateTime fecha) {
        if (estado == EstadoAdmision.DADA_DE_ALTA) {
            throw new IllegalStateException("Esta admisión ya fue dada de alta.");
        }
        if (tieneOrdenesLaboratorioPendientes) {
            throw new IllegalStateException(
                    "No se puede dar de alta: el paciente tiene órdenes de laboratorio pendientes.");
        }
        this.estado = EstadoAdmision.DADA_DE_ALTA;
        this.fechaAlta = fecha;
    }

    // Getters estilo JavaBean
    public String getId() {
        return id;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public String getSedeActualId() {
        return sedeActualId;
    }

    public EstadoAdmision getEstado() {
        return estado;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public List<Traslado> getHistorialTraslados() {
        return Collections.unmodifiableList(historialTraslados);
    }

    // Compatibilidad de métodos cortos
    public String id() {
        return getId();
    }

    public String pacienteId() {
        return getPacienteId();
    }

    public String sedeActualId() {
        return getSedeActualId();
    }

    public EstadoAdmision estado() {
        return getEstado();
    }

    public LocalDateTime fechaIngreso() {
        return getFechaIngreso();
    }

    public LocalDateTime fechaAlta() {
        return getFechaAlta();
    }

    public List<Traslado> historialTraslados() {
        return getHistorialTraslados();
    }
}