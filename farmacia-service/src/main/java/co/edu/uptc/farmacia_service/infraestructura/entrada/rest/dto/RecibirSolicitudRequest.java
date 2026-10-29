package co.edu.uptc.farmacia_service.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada del adaptador primario. Pertenece a infraestructura: el
 * núcleo nunca recibe esta forma de JSON directamente.
 */
public record RecibirSolicitudRequest(
        @NotBlank(message = "el id del paciente es obligatorio") String pacienteId,
        @NotBlank(message = "el id del médico es obligatorio") String medicoId,
        @NotBlank(message = "el medicamento es obligatorio") String medicamento) {
}
