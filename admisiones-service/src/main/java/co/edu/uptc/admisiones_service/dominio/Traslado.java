package co.edu.uptc.admisiones_service.dominio;

import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/**
 * Value Object: Traslado.
 *
 * Registra un movimiento de un paciente entre sedes, para cumplir el
 * criterio de aceptación de HU-03: "el historial del paciente conserva el
 * registro de todos sus traslados". Forma parte del Agregado Admision
 * (no tiene identidad ni se consulta por fuera de él).
 *
 * @Embeddable — igual que CorreoInstitucional en el taller de RICA: este
 *             import de jakarta.persistence es el único "framework" que se
 *             tolera en el
 *             paquete dominio (sección 5 de la guía de Hexagonal). Hibernate en
 *             Spring
 *             Boot 4.1 sí soporta records como tipo embebido, igual que ya se
 *             demostró
 *             con CorreoInstitucional.
 */
@Embeddable
public record Traslado(
        String sedeOrigenId,
        String sedeDestinoId,
        LocalDateTime fecha) {

    public Traslado {
        if (sedeOrigenId == null || sedeOrigenId.isBlank()) {
            throw new IllegalArgumentException("La sede de origen es obligatoria en un traslado.");
        }
        if (sedeDestinoId == null || sedeDestinoId.isBlank()) {
            throw new IllegalArgumentException("La sede de destino es obligatoria en un traslado.");
        }
        if (sedeOrigenId.equals(sedeDestinoId)) {
            throw new IllegalArgumentException("No tiene sentido trasladar al paciente a la misma sede.");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("El traslado debe tener fecha.");
        }
    }
}
