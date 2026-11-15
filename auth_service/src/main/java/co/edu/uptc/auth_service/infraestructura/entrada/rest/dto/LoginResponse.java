// infraestructura/entrada/rest/dto/LoginResponse.java
package co.edu.uptc.auth_service.infraestructura.entrada.rest.dto;

public record LoginResponse(String token, String rol, long expiraEnSegundos) {
}
