package co.edu.uptc.farmacia_service.infraestructura.salida.aseguradora;

import co.edu.uptc.farmacia_service.aplicacion.ConsultaCoberturaExterna;
import co.edu.uptc.farmacia_service.dominio.Cobertura;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Adaptador provisional (stub) del puerto ConsultaCoberturaExterna.
 *
 * Activo por defecto para que el servicio arranque y se pueda probar de
 * punta a punta sin la aseguradora real. Devuelve una cobertura fija del
 * 80% para cualquier paciente. Con el perfil "aseguradora-rest" se
 * sustituye por la implementación REST, sin tocar el núcleo.
 */
@Component
@Profile("!aseguradora-rest")
public class CoberturaAseguradoraStubAdapter implements ConsultaCoberturaExterna {

    @Override
    public Cobertura consultar(String pacienteId) {
        return new Cobertura(80);
    }
}
