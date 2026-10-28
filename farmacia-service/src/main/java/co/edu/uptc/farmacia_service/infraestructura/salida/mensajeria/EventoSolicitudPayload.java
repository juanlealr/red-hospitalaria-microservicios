package co.edu.uptc.farmacia_service.infraestructura.salida.mensajeria;

import java.time.LocalDateTime;

/**
 * Cuerpo JSON que se publica en el topic. Es un contrato del adaptador de
 * salida: el núcleo no lo conoce.
 */
public record EventoSolicitudPayload(
        String eventoId,
        String tipo,
        LocalDateTime ocurridoEn,
        String solicitudId,
        String pacienteId,
        String medicoId,
        String medicamento,
        String estado,
        Integer cobertura,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaDespacho) {
}
