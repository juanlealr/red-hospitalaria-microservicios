// auth-service/src/main/java/co/edu/uptc/auth_service/infraestructura/arranque/SembradorAdministradorInicial.java
package co.edu.uptc.auth_service.infraestructura.arranque;

import co.edu.uptc.auth_service.aplicacion.RepositorioUsuarios;
import co.edu.uptc.auth_service.dominio.Rol;
import co.edu.uptc.auth_service.dominio.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Crea el primer administrador del sistema la primera vez que el servicio
 * arranca y no existe ninguno todavía. Después de eso, cualquier otra
 * cuenta (médico, laboratorio, farmacéutico, admisiones, u OTROS
 * administradores) se crea desde /api/auth/usuarios, autenticado como
 * ADMINISTRADOR — nunca por registro público.
 */
@Component
public class SembradorAdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorAdministradorInicial.class);

    private final RepositorioUsuarios repositorio;
    private final PasswordEncoder passwordEncoder;
    private final String emailAdmin;
    private final String passwordAdmin;

    public SembradorAdministradorInicial(
            RepositorioUsuarios repositorio,
            PasswordEncoder passwordEncoder,
            @Value("${seguridad.admin-inicial.email:admin@hospital.com}") String emailAdmin,
            @Value("${seguridad.admin-inicial.password:CambiaEstaClave123!}") String passwordAdmin) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.emailAdmin = emailAdmin;
        this.passwordAdmin = passwordAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repositorio.buscarPorEmail(emailAdmin).isPresent()) {
            return; // ya existe, no se repite en próximos arranques
        }

        Usuario admin = new Usuario(
                UUID.randomUUID().toString(), emailAdmin, passwordEncoder.encode(passwordAdmin), Rol.ADMINISTRADOR);
        repositorio.guardar(admin);

        log.warn("Administrador inicial creado automáticamente: {} — "
                + "cámbiale la contraseña por defecto antes de usar esto fuera de desarrollo.", emailAdmin);
    }
}
