// aplicacion/RegistroUsuarioService.java
package co.edu.uptc.auth_service.aplicacion;

import co.edu.uptc.auth_service.compartido.CorreoYaRegistradoException;
import co.edu.uptc.auth_service.dominio.Rol;
import co.edu.uptc.auth_service.dominio.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistroUsuarioService {

    private final RepositorioUsuarios repositorio;
    private final PasswordEncoder passwordEncoder;

    public RegistroUsuarioService(RepositorioUsuarios repositorio, PasswordEncoder passwordEncoder) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrar(String email, String password, Rol rol) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio.");
        }
        if (repositorio.existePorEmail(email)) {
            throw new CorreoYaRegistradoException("Ya existe un usuario registrado con el correo " + email);
        }

        Usuario usuario = new Usuario(UUID.randomUUID().toString(), email, passwordEncoder.encode(password), rol);
        return repositorio.guardar(usuario);
    }
}
