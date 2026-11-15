// infraestructura/entrada/rest/dto/RegistroRequest.java
package co.edu.uptc.auth_service.infraestructura.entrada.rest.dto;

import co.edu.uptc.auth_service.dominio.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @Email @NotNull String email,
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres") String password,
        @NotNull Rol rol) {
}
