// infraestructura/entrada/rest/dto/LoginRequest.java
package co.edu.uptc.auth_service.infraestructura.entrada.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
