package co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia;

import co.edu.uptc.laboratorio_service.dominio.EstadoOrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de persistencia. El dominio (OrdenLaboratorio) se mantiene puro, sin
 * anotaciones JPA: este adaptador es el único que conoce las dos formas.
 */
@Entity
@Table(name = "ordenes_laboratorio")
public class OrdenLaboratorioEntity {

    @Id
    private UUID id;

    private String pacienteId;
    private String tipoExamen;

    @Enumerated(EnumType.STRING)
    private EstadoOrdenLaboratorio estado;

    private LocalDateTime fechaSolicitud;
    private String resultadoValor;
    private String resultadoUnidad;

    @Version
    private Long version;

    protected OrdenLaboratorioEntity() {
        // exigido por Hibernate
    }

    public static OrdenLaboratorioEntity desde(OrdenLaboratorio orden) {
        OrdenLaboratorioEntity entidad = new OrdenLaboratorioEntity();
        entidad.id = orden.getId();
        entidad.pacienteId = orden.getPacienteId();
        entidad.tipoExamen = orden.getTipoExamen();
        entidad.fechaSolicitud = orden.getFechaSolicitud();
        entidad.actualizarDesde(orden);
        return entidad;
    }

    /** Copia el estado mutable del dominio (conserva id y version de la fila). */
    public void actualizarDesde(OrdenLaboratorio orden) {
        this.estado = orden.getEstado();
        ResultadoExamen resultado = orden.getResultado();
        this.resultadoValor = resultado == null ? null : resultado.valor();
        this.resultadoUnidad = resultado == null ? null : resultado.unidad();
    }

    public OrdenLaboratorio aDominio() {
        ResultadoExamen resultado = resultadoValor == null
                ? null
                : new ResultadoExamen(resultadoValor, resultadoUnidad);
        return OrdenLaboratorio.reconstituir(
                id, pacienteId, tipoExamen, fechaSolicitud, estado, resultado);
    }
}
