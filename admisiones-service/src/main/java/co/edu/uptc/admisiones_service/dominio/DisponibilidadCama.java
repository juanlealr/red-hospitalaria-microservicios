package co.edu.uptc.admisiones_service.dominio;

/**
 * Value Object: DisponibilidadCama.
 *
 * Representa, para una sede y un servicio dados, cuántas camas hay en total
 * y cuántas están ocupadas en este momento (HU-02, HU-10).
 *
 * Es inmutable y no tiene identidad propia: dos DisponibilidadCama con los
 * mismos valores son intercambiables. Se valida en el constructor compacto
 * para que nunca exista una instancia en un estado inconsistente.
 */
public record DisponibilidadCama(
        String sedeId,
        String servicio,
        int camasTotales,
        int camasOcupadas) {

    public DisponibilidadCama {
        if (sedeId == null || sedeId.isBlank()) {
            throw new IllegalArgumentException("La sede es obligatoria para consultar disponibilidad de camas.");
        }
        if (servicio == null || servicio.isBlank()) {
            throw new IllegalArgumentException("El servicio es obligatorio para consultar disponibilidad de camas.");
        }
        if (camasTotales < 0) {
            throw new IllegalArgumentException("El total de camas no puede ser negativo.");
        }
        if (camasOcupadas < 0) {
            throw new IllegalArgumentException("Las camas ocupadas no pueden ser negativas.");
        }
        if (camasOcupadas > camasTotales) {
            throw new IllegalArgumentException(
                    "Las camas ocupadas (%d) no pueden superar el total de camas (%d) de la sede."
                            .formatted(camasOcupadas, camasTotales));
        }
    }

    /** Cuántas camas quedan libres ahora mismo. */
    public int camasDisponibles() {
        return camasTotales - camasOcupadas;
    }

    /**
     * Regla de negocio usada por TrasladoPacienteService y AdmisionFactory:
     * solo se considera "cama confirmada" cuando queda al menos una libre.
     */
    public boolean hayCamaConfirmada() {
        return camasDisponibles() > 0;
    }
}
