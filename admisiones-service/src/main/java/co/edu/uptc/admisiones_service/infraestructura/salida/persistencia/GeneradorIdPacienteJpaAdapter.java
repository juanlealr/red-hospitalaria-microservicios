package co.edu.uptc.admisiones_service.infraestructura.salida.persistencia;

import co.edu.uptc.admisiones_service.aplicacion.GeneradorIdPaciente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
public class GeneradorIdPacienteJpaAdapter implements GeneradorIdPaciente {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String siguiente() {
        Number n = (Number) em.createNativeQuery("select nextval('paciente_secuencia')").getSingleResult();
        return "PAC-" + n.longValue();
    }
}
