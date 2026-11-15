package co.edu.uptc.consultas_admisiones_service.dominio.repositorio;

import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaMedicacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FichaMedicacionRepository extends MongoRepository<FichaMedicacion, String> {
    List<FichaMedicacion> findByPacienteId(String pacienteId);
}