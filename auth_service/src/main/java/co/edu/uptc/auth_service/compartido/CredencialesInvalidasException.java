// compartido/CredencialesInvalidasException.java
package co.edu.uptc.auth_service.compartido;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
