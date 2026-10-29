package co.edu.uptc.farmacia_service.infraestructura.entrada.rest.dto;

import java.time.LocalDateTime;

/**
 * DTO de salida del adaptador primario. Traduce el Agregado a la forma que
 * expone la API, sin filtrar el modelo de dominio.
 */
public record SolicitudMedicamentoResponse(
        String id,
        String pacienteId,
        String medicoId,
        String medicamento,
        String estado,
        Integer cobertura,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaDespacho) {
}
