package co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarResultadoRequest(
        @NotBlank(message = "El valor del resultado es obligatorio")
        @Size(max = 255, message = "El valor no puede superar 255 caracteres") String valor,
        @NotBlank(message = "La unidad del resultado es obligatoria")
        @Size(max = 50, message = "La unidad no puede superar 50 caracteres") String unidad) {
}
