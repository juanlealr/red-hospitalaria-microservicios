package co.edu.uptc.admisiones_service.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Raíz del Agregado: Admision.
 *
 * Es la única clase con @Entity en el paquete dominio — igual que
 * Investigador en RICA. Límite del Agregado (mini-ADR en NOTAS-equipo.md):
 * todo lo que cambia junto y de forma consistente vive aquí (estado,
 * sede actual, historial de traslados). Admision NUNCA referencia
 * entidades completas de otros subdominios: al Paciente lo referencia solo
 * por pacienteId, y para dar de alta (HU-09) no conoce la entidad
 * OrdenLaboratorio del subdominio de Laboratorio — solo recibe un booleano
 * ya resuelto por quien orquesta el caso de uso (AdmisionService).
 *
 * Nota sobre visibilidad tras la reorganización hexagonal: en el taller de
 * DDD (paquete plano) el constructor y registrarTraslado podían ser de
 * paquete porque Admision y su Factory vivían juntas. Al separar en
 * dominio/aplicacion (sin JPMS — la guía es explícita en que no hay regla
 * automática que impida un import indebido) ya no comparten paquete, así
 * que ambos pasan a ser public. La restricción de "nadie más debería
 * construir un Admision directamente" ya no la impone el compilador — la
 * impone la revisión de Pull Request (sección 8, checklist de proyecto).
 * El constructor vacío es exclusivamente para que Hibernate reconstruya el
 * objeto desde la base de datos.
 */
@Entity
@Table(name = "admisiones")
public class Admision {

    @Id
    private String id;

    private String pacienteId;

    private String sedeActualId;

    @Enumerated(EnumType.STRING)
    private EstadoAdmision estado;

    private LocalDateTime fechaIngreso;

    private LocalDateTime fechaAlta;

    @ElementCollection
    @CollectionTable(name = "admision_traslados", joinColumns = @JoinColumn(name = "admision_id"))
    private List<Traslado> historialTraslados = new ArrayList<>();

    protected Admision() {
        // exigido por Hibernate; nunca se invoca desde código de aplicación
    }

    /**
     * Solo debe llamarse desde AdmisionFactory — ver nota de visibilidad arriba.
     */
    public Admision(String id, String pacienteId, String sedeInicialId, LocalDateTime fechaIngreso) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.sedeActualId = sedeInicialId;
        this.estado = EstadoAdmision.ACTIVA;
        this.fechaIngreso = fechaIngreso;
    }

    /**
     * Solo debe llamarse desde TrasladoPacienteService — ver nota de
     * visibilidad arriba. Usado una vez confirmó cama en destino (HU-03).
     */
    public void registrarTraslado(String sedeDestinoId, LocalDateTime fecha) {
        if (estado != EstadoAdmision.ACTIVA) {
            throw new IllegalStateException("Solo se puede trasladar una admisión activa.");
        }
        historialTraslados.add(new Traslado(this.sedeActualId, sedeDestinoId, fecha));
        this.sedeActualId = sedeDestinoId;
    }

    /**
     * Alta médica y cierre de la admisión (HU-09).
     * Criterio de aceptación: se rechaza si el paciente tiene órdenes de
     * laboratorio pendientes sin resultado.
     */
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