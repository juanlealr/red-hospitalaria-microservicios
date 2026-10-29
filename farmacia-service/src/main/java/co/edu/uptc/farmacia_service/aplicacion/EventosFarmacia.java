package co.edu.uptc.farmacia_service.aplicacion;

/**
 * Nombres de los eventos de dominio del subdominio Farmacia y cobertura.
 * Salen del Event Storming del equipo (NOTAS-equipo.md) y son los que se
 * publican en el topic de Kafka a través del Transactional Outbox.
 */
public final class EventosFarmacia {

    public static final String MEDICAMENTO_SOLICITADO = "MedicamentoSolicitado";
    public static final String COBERTURA_VERIFICADA = "CoberturaVerificada";
    public static final String MEDICAMENTO_DESPACHADO = "MedicamentoDespachado";

    private EventosFarmacia() {
    }
}
