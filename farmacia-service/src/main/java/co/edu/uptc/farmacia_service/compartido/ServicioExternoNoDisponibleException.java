package co.edu.uptc.farmacia_service.compartido;

/**
 * La integración con un sistema externo (la aseguradora) falló o el
 * Circuit Breaker está abierto. Se traduce a HTTP 503 en el adaptador
 * primario.
 */
public class ServicioExternoNoDisponibleException extends RuntimeException {

    public ServicioExternoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
