package co.edu.uptc.laboratorio_service.dominio;

import java.util.Arrays;
import java.util.List;

/**
 * Ciclo de vida de una orden de laboratorio (HU-04, HU-05).
 *
 * Aquí vive —en un solo lugar— la definición de "orden pendiente" que usa
 * Admisiones para decidir el alta (HU-09): una orden está pendiente mientras
 * no esté CERRADA. Si el equipo decide que "pendiente" es solo SOLICITADA,
 * se cambia únicamente la bandera de CON_RESULTADO.
 */
public enum EstadoOrdenLaboratorio {

    SOLICITADA(true),
    CON_RESULTADO(true),
    CERRADA(false);

    private final boolean pendiente;

    EstadoOrdenLaboratorio(boolean pendiente) {
        this.pendiente = pendiente;
    }

    public boolean esPendiente() {
        return pendiente;
    }

    public static List<EstadoOrdenLaboratorio> pendientes() {
        return Arrays.stream(values())
                .filter(EstadoOrdenLaboratorio::esPendiente)
                .toList();
    }
}
