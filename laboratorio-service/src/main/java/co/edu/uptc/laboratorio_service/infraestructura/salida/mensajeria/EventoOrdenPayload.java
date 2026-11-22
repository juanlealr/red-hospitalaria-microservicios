package co.edu.uptc.laboratorio_service.infraestructura.salida.mensajeria;

/**
 * Cuerpo JSON que se publica en el topic {@code laboratorio.ordenes}. Es el contrato
 * con los consumidores (p. ej. consultas_admisiones_service para el estado consolidado,
 * HU-08): no cambies nombres de campos sin avisarles.
 *
 * Las fechas viajan como texto ISO-8601 (LocalDateTime.toString()), igual que los
 * eventos de admisiones, para no depender de cómo configure Jackson cada consumidor.
 * resultadoValor / resultadoUnidad son null mientras la orden está SOLICITADA.
 */
public record EventoOrdenPayload(
        String eventoId,
        String tipo,
        String ocurridoEn,
        String ordenId,
        String pacienteId,
        String tipoExamen,
        String estado,
        String resultadoValor,
        String resultadoUnidad,
        String fechaSolicitud) {
}
