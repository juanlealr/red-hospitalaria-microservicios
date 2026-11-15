// aplicacion/RepositorioUsuarios.java
package co.edu.uptc.auth_service.aplicacion;

import co.edu.uptc.auth_service.dominio.Usuario;
import java.util.Optional;

public interface RepositorioUsuarios {
    boolean existePorEmail(String email);

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorEmail(String email);
}
