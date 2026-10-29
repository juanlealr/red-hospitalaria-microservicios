package co.edu.uptc.farmacia_service.dominio;

/**
 * Estados posibles de una SolicitudMedicamento, dentro del subdominio
 * "Farmacia y cobertura". Coinciden uno a uno con los eventos de dominio
 * identificados en el Event Storming del equipo (NOTAS-equipo.md):
 * MedicamentoSolicitado -> CoberturaVerificada -> MedicamentoDespachado.
 */
public enum EstadoSolicitudMedicamento {

    /** La solicitud fue recibida por farmacia (HU-06). */
    RECIBIDA,

    /** La aseguradora externa ya confirmó qué porcentaje cubre (HU-07). */
    COBERTURA_VERIFICADA,

    /** El medicamento fue despachado al paciente (HU-06). */
    DESPACHADA
}
