// compartido/CorreoYaRegistradoException.java
package co.edu.uptc.auth_service.compartido;

public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException(String mensaje) {
        super(mensaje);
    }
}