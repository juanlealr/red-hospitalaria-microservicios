// aplicacion/AutenticacionService.java
package co.edu.uptc.auth_service.aplicacion;

import co.edu.uptc.auth_service.compartido.CredencialesInvalidasException;
import co.edu.uptc.auth_service.dominio.Usuario;
import co.edu.uptc.auth_service.infraestructura.seguridad.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacionService {

    private final RepositorioUsuarios repositorio;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AutenticacionService(RepositorioUsuarios repositorio, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public ResultadoAutenticacion autenticar(String email, String password) {
        Usuario usuario = repositorio.buscarPorEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));

        if (!passwordEncoder.matches(password, usuario.passwordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }

        String token = jwtService.generar(usuario.email(), usuario.rol());
        return new ResultadoAutenticacion(token, usuario.rol().name(), jwtService.expiracionSegundos());
    }
}
