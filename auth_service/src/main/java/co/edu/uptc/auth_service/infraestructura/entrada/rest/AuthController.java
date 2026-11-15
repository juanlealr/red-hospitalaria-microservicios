package co.edu.uptc.auth_service.infraestructura.entrada.rest;

import co.edu.uptc.auth_service.aplicacion.AutenticacionService;
import co.edu.uptc.auth_service.aplicacion.RegistroUsuarioService;
import co.edu.uptc.auth_service.aplicacion.ResultadoAutenticacion;
import co.edu.uptc.auth_service.dominio.Usuario;
import co.edu.uptc.auth_service.infraestructura.entrada.rest.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistroUsuarioService registroUsuarioService;
    private final AutenticacionService autenticacionService;

    public AuthController(RegistroUsuarioService registroUsuarioService, AutenticacionService autenticacionService) {
        this.registroUsuarioService = registroUsuarioService;
        this.autenticacionService = autenticacionService;
    }

    /** Público: la única puerta de entrada sin credenciales previas. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        ResultadoAutenticacion resultado = autenticacionService.autenticar(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(resultado.token(), resultado.rol(), resultado.expiraEnSegundos()));
    }

    /**
     * Protegido: solo un ADMINISTRADOR ya autenticado puede crear cuentas
     * de personal (médico, admisiones, laboratorio, farmacéutico, u otro
     * administrador). Esto reemplaza al antiguo "registro público".
     */
    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = registroUsuarioService.registrar(request.email(), request.password(), request.rol());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsuarioResponse(usuario.id(), usuario.email(), usuario.rol().name()));
    }
}
