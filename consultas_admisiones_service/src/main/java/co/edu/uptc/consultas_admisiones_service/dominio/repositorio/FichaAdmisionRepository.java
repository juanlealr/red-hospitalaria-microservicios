package co.edu.uptc.consultas_admisiones_service.dominio.repositorio;

import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaAdmision;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FichaAdmisionRepository extends MongoRepository<FichaAdmision, String> {
    List<FichaAdmision> findByPacienteId(String pacienteId);

    List<FichaAdmision> findByEstado(String estado);

    List<FichaAdmision> findBySedeActualIdAndEstado(String sedeActualId, String estado);

    Optional<FichaAdmision> findFirstByPacienteIdAndEstado(String pacienteId, String estado);
}
