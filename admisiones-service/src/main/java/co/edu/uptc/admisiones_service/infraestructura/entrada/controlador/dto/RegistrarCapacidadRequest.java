package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RegistrarCapacidadRequest(
        @NotBlank(message = "La sede es obligatoria") String sedeId,
        @NotBlank(message = "El servicio es obligatorio") String servicio,
        @Min(value = 0, message = "El total de camas no puede ser negativo") int camasTotales) {
}
