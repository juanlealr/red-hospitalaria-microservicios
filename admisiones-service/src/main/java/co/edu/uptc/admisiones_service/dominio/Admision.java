package co.edu.uptc.admisiones_service.dominio;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "admisiones")
public class Admision {

    @Id
    private String id;

    private String pacienteId;
    private String sedeActualId;
    private String servicioActual;

    @Enumerated(EnumType.STRING)
    private EstadoAdmision estado;

    private LocalDateTime fechaIngreso;
    private LocalDateTime fechaAlta;

    @ElementCollection
    @CollectionTable(name = "admision_traslados", joinColumns = @JoinColumn(name = "admision_id"))
    private List<Traslado> historialTraslados = new ArrayList<>();

    protected Admision() {
        // exigido por Hibernate
    }

    public Admision(String id, String pacienteId, String sedeInicialId, String servicioInicial,
            LocalDateTime fechaIngreso) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.sedeActualId = sedeInicialId;
        this.servicioActual = servicioInicial;
        this.estado = EstadoAdmision.ACTIVA;
        this.fechaIngreso = fechaIngreso;
    }

    public void registrarTraslado(String sedeDestinoId, String servicioDestino, LocalDateTime fecha) {
        if (estado != EstadoAdmision.ACTIVA) {
            throw new IllegalStateException("Solo se puede trasladar una admisión activa.");
        }
        historialTraslados.add(new Traslado(
                this.sedeActualId, this.servicioActual, sedeDestinoId, servicioDestino, fecha));
        this.sedeActualId = sedeDestinoId;
        this.servicioActual = servicioDestino;
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

    public String id() {
        return id;
    }

    public String pacienteId() {
        return pacienteId;
    }

    public String sedeActualId() {
        return sedeActualId;
    }

    public String servicioActual() {
        return servicioActual;
    }

    public EstadoAdmision estado() {
        return estado;
    }

    public LocalDateTime fechaIngreso() {
        return fechaIngreso;
    }

    public LocalDateTime fechaAlta() {
        return fechaAlta;
    }

    public List<Traslado> historialTraslados() {
        return Collections.unmodifiableList(historialTraslados);
    }
}