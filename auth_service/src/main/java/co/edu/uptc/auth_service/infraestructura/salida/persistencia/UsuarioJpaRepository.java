// infraestructura/salida/persistencia/UsuarioJpaRepository.java
package co.edu.uptc.auth_service.infraestructura.salida.persistencia;

import co.edu.uptc.auth_service.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, String> {
    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}