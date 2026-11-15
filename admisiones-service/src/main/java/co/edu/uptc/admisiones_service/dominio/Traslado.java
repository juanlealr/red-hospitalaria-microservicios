package co.edu.uptc.admisiones_service.dominio;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public record Traslado(
        String sedeOrigenId,
        String servicioOrigen,
        String sedeDestinoId,
        String servicioDestino,
        LocalDateTime fecha) {

    public Traslado {
        if (sedeOrigenId == null || sedeOrigenId.isBlank()) {
            throw new IllegalArgumentException("La sede de origen es obligatoria en un traslado.");
        }
        if (sedeDestinoId == null || sedeDestinoId.isBlank()) {
            throw new IllegalArgumentException("La sede de destino es obligatoria en un traslado.");
        }
        if (sedeOrigenId.equals(sedeDestinoId) && servicioOrigen != null && servicioOrigen.equals(servicioDestino)) {
            throw new IllegalArgumentException("No tiene sentido trasladar al paciente al mismo sitio.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("El traslado debe tener fecha.");
        }
    }
}
