// aplicacion/ResultadoAutenticacion.java
package co.edu.uptc.auth_service.aplicacion;

public record ResultadoAutenticacion(String token, String rol, long expiraEnSegundos) {
}