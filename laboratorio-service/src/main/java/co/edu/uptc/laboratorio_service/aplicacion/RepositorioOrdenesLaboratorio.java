package co.edu.uptc.laboratorio_service.aplicacion;

import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario: solo lo que los casos de uso realmente necesitan.
 */
public interface RepositorioOrdenesLaboratorio {

    OrdenLaboratorio guardar(OrdenLaboratorio orden);

    Optional<OrdenLaboratorio> buscarPorId(UUID id);

    List<OrdenLaboratorio> listarTodas();

    List<OrdenLaboratorio> listarPorPaciente(String pacienteId);

    /** HU-09: ¿el paciente tiene al menos una orden sin cerrar? */
    boolean existenPendientesDelPaciente(String pacienteId);
}
