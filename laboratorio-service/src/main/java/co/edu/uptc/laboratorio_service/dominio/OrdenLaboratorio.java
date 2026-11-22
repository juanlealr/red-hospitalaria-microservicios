package co.edu.uptc.laboratorio_service.dominio;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Raíz del agregado OrdenLaboratorio. Protege sus invariantes:
 * no se registra resultado en una orden cerrada, no se cierra sin
 * resultado y no se cierra dos veces.
 *
 * El paciente se referencia solo por id (String, el mismo formato que
 * genera admisiones-service, p. ej. "PAC-1001"): este contexto nunca
 * conoce al agregado Paciente.
 */
public class OrdenLaboratorio {

    private final UUID id;
    private final String pacienteId;
    private final String tipoExamen;
    private final LocalDateTime fechaSolicitud;

    private EstadoOrdenLaboratorio estado;
    private ResultadoExamen resultado;

    OrdenLaboratorio(
            UUID id,
            String pacienteId,
            String tipoExamen,
            LocalDateTime fechaSolicitud
    ) {

        this.id = Objects.requireNonNull(
                id,
                "El id de la orden es obligatorio."
        );

        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException(
                    "El id del paciente es obligatorio."
            );
        }

        this.pacienteId = pacienteId.trim();

        if (tipoExamen == null || tipoExamen.isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de examen es obligatorio."
            );
        }

        this.tipoExamen = tipoExamen.trim();

        this.fechaSolicitud = Objects.requireNonNull(
                fechaSolicitud,
                "La fecha de solicitud es obligatoria."
        );

        this.estado = EstadoOrdenLaboratorio.SOLICITADA;
        this.resultado = null;
    }

    /**
     * Reconstruye una orden que ya existía (por ejemplo, al leerla de la
     * base de datos) sin pasar por la factory. Valida que el estado y el
     * resultado sean coherentes entre sí.
     */
    public static OrdenLaboratorio reconstituir(
            UUID id,
            String pacienteId,
            String tipoExamen,
            LocalDateTime fechaSolicitud,
            EstadoOrdenLaboratorio estado,
            ResultadoExamen resultado
    ) {

        Objects.requireNonNull(estado, "El estado de la orden es obligatorio.");

        if (estado == EstadoOrdenLaboratorio.SOLICITADA && resultado != null) {
            throw new IllegalArgumentException(
                    "Una orden SOLICITADA no puede tener resultado."
            );
        }

        if (estado != EstadoOrdenLaboratorio.SOLICITADA && resultado == null) {
            throw new IllegalArgumentException(
                    "Una orden " + estado + " debe tener resultado."
            );
        }

        OrdenLaboratorio orden =
                new OrdenLaboratorio(id, pacienteId, tipoExamen, fechaSolicitud);
        orden.estado = estado;
        orden.resultado = resultado;
        return orden;
    }

    public UUID getId() {
        return id;
    }

    public String getPacienteId() {
        return pacienteId;
    }

    public String getTipoExamen() {
        return tipoExamen;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoOrdenLaboratorio getEstado() {
        return estado;
    }

    public ResultadoExamen getResultado() {
        return resultado;
    }

    public boolean tieneResultado() {
        return resultado != null;
    }

    /** Una orden está pendiente mientras no esté cerrada (ver EstadoOrdenLaboratorio). */
    public boolean estaPendiente() {
        return estado.esPendiente();
    }

    public void registrarResultado(ResultadoExamen resultado) {

        Objects.requireNonNull(
                resultado,
                "El resultado del examen es obligatorio."
        );

        if (estado == EstadoOrdenLaboratorio.CERRADA) {
            throw new IllegalStateException(
                    "No se puede registrar un resultado en una orden cerrada."
            );
        }

        this.resultado = resultado;
        this.estado = EstadoOrdenLaboratorio.CON_RESULTADO;
    }

    public void cerrar() {

        if (!tieneResultado()) {
            throw new IllegalStateException(
                    "No se puede cerrar una orden sin resultado registrado."
            );
        }

        if (estado == EstadoOrdenLaboratorio.CERRADA) {
            throw new IllegalStateException(
                    "La orden de laboratorio ya está cerrada."
            );
        }

        this.estado = EstadoOrdenLaboratorio.CERRADA;
    }

    /** Una entidad se identifica por su id, no por sus atributos. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrdenLaboratorio otra)) {
            return false;
        }
        return id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
