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
    private String medicoTratante;
    private String motivoIngreso;
    private Integer coberturaPorcentaje;

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

    public Admision(
            String id,
            String pacienteId,
            String sedeInicialId,
            String servicioInicial,
            String medicoTratante,
            String motivoIngreso,
            LocalDateTime fechaIngreso) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.sedeActualId = sedeInicialId;
        this.servicioActual = servicioInicial;
        this.medicoTratante = medicoTratante;
        this.motivoIngreso = motivoIngreso;
        this.estado = EstadoAdmision.ACTIVA;
        this.fechaIngreso = fechaIngreso;
    }

    public void registrarCobertura(int porcentaje) {
        if (estado != EstadoAdmision.ACTIVA) {
            throw new IllegalStateException("Solo se puede verificar cobertura en una admisión activa.");
        }
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException(
                    "El porcentaje de cobertura debe estar entre 0 y 100 (recibido: %d).".formatted(porcentaje));
        }
        this.coberturaPorcentaje = porcentaje;
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

    public String medicoTratante() {
        return medicoTratante;
    }

    public String motivoIngreso() {
        return motivoIngreso;
    }

    public Integer coberturaPorcentaje() {
        return coberturaPorcentaje;
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