package co.edu.uptc.farmacia_service.dominio;

import java.time.LocalDateTime;

/**
 * Raíz del Agregado: SolicitudMedicamento.
 *
 * Es el único punto de entrada del subdominio "Farmacia y cobertura". Todo
 * lo que cambia junto y de forma consistente vive aquí: el estado de la
 * solicitud (RECIBIDA -> COBERTURA_VERIFICADA -> DESPACHADA) y la cobertura
 * verificada.
 *
 * Referencias a otros subdominios: siempre por id, nunca por objeto
 * completo (pacienteId, medicoId). El núcleo no importa Spring, JPA ni
 * ningún cliente HTTP: la persistencia y la aseguradora entran por puertos.
 */
public class SolicitudMedicamento {

    private final String id;

    private final String pacienteId;

    private final String medicoId;

    private final String medicamento;

    private final LocalDateTime fechaSolicitud;

    private EstadoSolicitudMedicamento estado;

    private Cobertura cobertura;

    private LocalDateTime fechaDespacho;

    /**
     * Solo debe llamarse desde SolicitudMedicamentoFactory (o desde
     * {@link #reconstruir} al rehidratar desde persistencia).
     */
    public SolicitudMedicamento(
            String id,
            String pacienteId,
            String medicoId,
            String medicamento,
            LocalDateTime fechaSolicitud) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.medicamento = medicamento;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = EstadoSolicitudMedicamento.RECIBIDA;
    }

    /**
     * Rehidrata el Agregado desde persistencia sin volver a ejecutar las
     * transiciones de estado (que fijarían fechas nuevas). Uso exclusivo de
     * los adaptadores de salida; valida que la combinación estado/cobertura/
     * fecha sea coherente para no reconstruir una solicitud imposible.
     */
    public static SolicitudMedicamento reconstruir(
            String id,
            String pacienteId,
            String medicoId,
            String medicamento,
            LocalDateTime fechaSolicitud,
            EstadoSolicitudMedicamento estado,
            Cobertura cobertura,
            LocalDateTime fechaDespacho) {
        exigirTexto(id, "El id de la solicitud es obligatorio.");
        exigirTexto(pacienteId, "El id del paciente es obligatorio.");
        exigirTexto(medicoId, "El id del médico es obligatorio.");
        exigirTexto(medicamento, "El medicamento es obligatorio.");
        if (fechaSolicitud == null) {
            throw new IllegalArgumentException("La fecha de solicitud es obligatoria.");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la solicitud es obligatorio.");
        }
        switch (estado) {
            case RECIBIDA -> {
                if (cobertura != null || fechaDespacho != null) {
                    throw new IllegalStateException("Una solicitud RECIBIDA no puede tener cobertura ni fecha de despacho.");
                }
            }
            case COBERTURA_VERIFICADA -> {
                if (cobertura == null) {
                    throw new IllegalStateException("Una solicitud con cobertura verificada debe tener cobertura.");
                }
                if (fechaDespacho != null) {
                    throw new IllegalStateException("Una solicitud no despachada no puede tener fecha de despacho.");
                }
            }
            case DESPACHADA -> {
                if (cobertura == null) {
                    throw new IllegalStateException("Una solicitud DESPACHADA debe tener cobertura verificada.");
                }
                if (fechaDespacho == null) {
                    throw new IllegalStateException("Una solicitud DESPACHADA debe tener fecha de despacho.");
                }
            }
        }

        SolicitudMedicamento solicitud = new SolicitudMedicamento(id, pacienteId, medicoId, medicamento, fechaSolicitud);
        solicitud.estado = estado;
        solicitud.cobertura = cobertura;
        solicitud.fechaDespacho = fechaDespacho;
        return solicitud;
    }

    /**
     * Registra la cobertura que confirmó la aseguradora externa (HU-07).
     * Solo tiene sentido sobre una solicitud recién recibida: una vez
     * verificada, la cobertura no se reemplaza.
     */
    public void registrarCobertura(Cobertura cobertura) {
        if (cobertura == null) {
            throw new IllegalArgumentException("La cobertura verificada es obligatoria.");
        }
        if (estado != EstadoSolicitudMedicamento.RECIBIDA) {
            throw new IllegalStateException(
                    "Solo se registra la cobertura de una solicitud recibida (estado actual: " + estado + ").");
        }
        this.cobertura = cobertura;
        this.estado = EstadoSolicitudMedicamento.COBERTURA_VERIFICADA;
    }

    /**
     * Despacho del medicamento al paciente (HU-06). Exige que la cobertura
     * ya haya sido verificada.
     */
    public void despachar(LocalDateTime fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("El despacho debe tener fecha.");
        }
        if (estado == EstadoSolicitudMedicamento.DESPACHADA) {
            throw new IllegalStateException("La solicitud ya fue despachada.");
        }
        if (estado != EstadoSolicitudMedicamento.COBERTURA_VERIFICADA) {
            throw new IllegalStateException(
                    "No se puede despachar sin cobertura verificada (estado actual: " + estado + ").");
        }
        this.estado = EstadoSolicitudMedicamento.DESPACHADA;
        this.fechaDespacho = fecha;
    }

    private static void exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }

    public String id() {
        return id;
    }

    public String pacienteId() {
        return pacienteId;
    }

    public String medicoId() {
        return medicoId;
    }

    public String medicamento() {
        return medicamento;
    }

    public LocalDateTime fechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoSolicitudMedicamento estado() {
        return estado;
    }

    public Cobertura cobertura() {
        return cobertura;
    }

    public LocalDateTime fechaDespacho() {
        return fechaDespacho;
    }
}
