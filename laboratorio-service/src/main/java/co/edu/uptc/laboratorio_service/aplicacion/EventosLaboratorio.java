package co.edu.uptc.laboratorio_service.aplicacion;

/**
 * Nombres de los eventos de dominio del contexto Laboratorio. Se publican en el
 * topic {@code laboratorio.ordenes} a través del Transactional Outbox; el nombre
 * viaja en el campo {@code tipo} del JSON (mismo esquema que farmacia.solicitudes).
 */
public final class EventosLaboratorio {

    /** HU-04: un médico solicitó un examen. */
    public static final String EXAMEN_SOLICITADO = "ExamenSolicitado";
    /** HU-05: el laboratorio registró el resultado. */
    public static final String RESULTADO_REGISTRADO = "ResultadoRegistrado";
    /** La orden se cerró: deja de bloquear el alta (HU-09). */
    public static final String ORDEN_CERRADA = "OrdenCerrada";

    private EventosLaboratorio() {
    }
}
