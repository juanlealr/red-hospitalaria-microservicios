// infraestructura/salida/persistencia/RepositorioUsuariosJpaAdapter.java
package co.edu.uptc.auth_service.infraestructura.salida.persistencia;

import co.edu.uptc.auth_service.aplicacion.RepositorioUsuarios;
import co.edu.uptc.auth_service.dominio.Usuario;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class RepositorioUsuariosJpaAdapter implements RepositorioUsuarios {

    private final UsuarioJpaRepository jpaRepository;

    public RepositorioUsuariosJpaAdapter(UsuarioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existePorEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return jpaRepository.save(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpaRepository.findByEmail(email);
    }
}
