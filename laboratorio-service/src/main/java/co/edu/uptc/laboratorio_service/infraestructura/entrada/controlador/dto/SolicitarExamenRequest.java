package co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarExamenRequest(
        @NotBlank(message = "El id del paciente es obligatorio") String pacienteId,
        @NotBlank(message = "El tipo de examen es obligatorio")
        @Size(max = 150, message = "El tipo de examen no puede superar 150 caracteres") String tipoExamen) {
}
