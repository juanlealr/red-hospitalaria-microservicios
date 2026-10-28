package co.edu.uptc.farmacia_service.dominio;

/**
 * Value Object: Cobertura.
 *
 * Representa el porcentaje que la aseguradora externa cubre para un
 * paciente (HU-07). Es inmutable y no tiene identidad propia: dos Cobertura
 * con el mismo porcentaje son intercambiables. Se valida en el constructor
 * compacto para que nunca exista una instancia con un porcentaje fuera del
 * rango 0..100.
 */
public record Cobertura(int porcentaje) {

    public Cobertura {
        if (porcentaje < 0 || porcentaje > 100) {
            throw new IllegalArgumentException(
                    "El porcentaje de cobertura debe estar entre 0 y 100 (recibido: %d).".formatted(porcentaje));
        }
    }
}
