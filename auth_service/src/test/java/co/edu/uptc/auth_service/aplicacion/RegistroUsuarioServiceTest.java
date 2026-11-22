package co.edu.uptc.auth_service.aplicacion;

import co.edu.uptc.auth_service.compartido.CorreoYaRegistradoException;
import co.edu.uptc.auth_service.dominio.Rol;
import co.edu.uptc.auth_service.dominio.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroUsuarioServiceTest {

    /** Repositorio en memoria: el servicio solo conoce el puerto, no la base de datos. */
    static class RepositorioEnMemoria implements RepositorioUsuarios {
        final Map<String, Usuario> porEmail = new HashMap<>();

        @Override
        public boolean existePorEmail(String email) {
            return porEmail.containsKey(email);
        }

        @Override
        public Usuario guardar(Usuario usuario) {
            porEmail.put(usuario.email(), usuario);
            return usuario;
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return Optional.ofNullable(porEmail.get(email));
        }
    }

    private RepositorioEnMemoria repositorio;
    private PasswordEncoder encoder;
    private RegistroUsuarioService servicio;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        encoder = new BCryptPasswordEncoder();
        servicio = new RegistroUsuarioService(repositorio, encoder);
    }

    @Test
    void registraUsuarioGuardandoLaContrasenaCifrada() {
        Usuario creado = servicio.registrar("medico@hospital.com", "clave-segura-1", Rol.MEDICO);

        assertEquals("medico@hospital.com", creado.email());
        assertEquals(Rol.MEDICO, creado.rol());
        assertNotEquals("clave-segura-1", creado.passwordHash());
        assertTrue(encoder.matches("clave-segura-1", creado.passwordHash()));
    }

    @Test
    void rechazaCorreoDuplicado() {
        servicio.registrar("a@hospital.com", "12345678", Rol.ADMISIONES);

        assertThrows(CorreoYaRegistradoException.class,
                () -> servicio.registrar("a@hospital.com", "87654321", Rol.MEDICO));
    }

    @Test
    void rechazaContrasenaCorta() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar("b@hospital.com", "corta", Rol.MEDICO));
    }

    @Test
    void rechazaCorreoVacioYRolNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar(" ", "12345678", Rol.MEDICO));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.registrar("c@hospital.com", "12345678", null));
    }
}
