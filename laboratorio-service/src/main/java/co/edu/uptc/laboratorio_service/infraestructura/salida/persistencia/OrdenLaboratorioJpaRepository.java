package co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia;

import co.edu.uptc.laboratorio_service.dominio.EstadoOrdenLaboratorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Detalle técnico: solo OrdenLaboratorioRepositoryJpaAdapter debería
 * conocer esta interfaz. El núcleo conoce RepositorioOrdenesLaboratorio.
 */
public interface OrdenLaboratorioJpaRepository extends JpaRepository<OrdenLaboratorioEntity, UUID> {

    List<OrdenLaboratorioEntity> findAllByOrderByFechaSolicitudDesc();

    List<OrdenLaboratorioEntity> findByPacienteIdOrderByFechaSolicitudDesc(String pacienteId);

    boolean existsByPacienteIdAndEstadoIn(String pacienteId, Collection<EstadoOrdenLaboratorio> estados);
}
