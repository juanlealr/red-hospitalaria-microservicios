package co.edu.uptc.auth_service.aplicacion;

import co.edu.uptc.auth_service.compartido.CredencialesInvalidasException;
import co.edu.uptc.auth_service.dominio.Rol;
import co.edu.uptc.auth_service.dominio.Usuario;
import co.edu.uptc.auth_service.infraestructura.seguridad.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AutenticacionServiceTest {

    private AutenticacionService servicio;

    @BeforeEach
    void preparar() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        Usuario medico = new Usuario("1", "medico@hospital.com", encoder.encode("12345678"), Rol.MEDICO);
        RepositorioUsuarios repositorio = new RepositorioUsuarios() {
            @Override
            public boolean existePorEmail(String email) {
                return medico.email().equals(email);
            }

            @Override
            public Usuario guardar(Usuario usuario) {
                return usuario;
            }

            @Override
            public Optional<Usuario> buscarPorEmail(String email) {
                return medico.email().equals(email) ? Optional.of(medico) : Optional.empty();
            }
        };
        JwtService jwt = new JwtService("secreto-de-pruebas-con-al-menos-32-bytes-1234567890", 60_000);
        servicio = new AutenticacionService(repositorio, encoder, jwt);
    }

    @Test
    void credencialesCorrectasDevuelvenTokenYRol() {
        ResultadoAutenticacion r = servicio.autenticar("medico@hospital.com", "12345678");

        assertEquals("MEDICO", r.rol());
        assertFalse(r.token().isBlank());
        assertEquals(60, r.expiraEnSegundos());
    }

    @Test
    void contrasenaIncorrectaSeRechaza() {
        assertThrows(CredencialesInvalidasException.class,
                () -> servicio.autenticar("medico@hospital.com", "otra-clave"));
    }

    @Test
    void usuarioInexistenteSeRechazaConElMismoError() {
        CredencialesInvalidasException ex = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.autenticar("nadie@hospital.com", "12345678"));
        assertEquals("Correo o contraseña incorrectos.", ex.getMessage());
    }
}
