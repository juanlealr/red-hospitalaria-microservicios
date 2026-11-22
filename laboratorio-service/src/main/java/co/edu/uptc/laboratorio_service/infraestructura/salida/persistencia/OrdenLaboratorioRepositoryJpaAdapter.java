package co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia;

import co.edu.uptc.laboratorio_service.aplicacion.RepositorioOrdenesLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.EstadoOrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrdenLaboratorioRepositoryJpaAdapter implements RepositorioOrdenesLaboratorio {

    private final OrdenLaboratorioJpaRepository jpaRepository;

    public OrdenLaboratorioRepositoryJpaAdapter(OrdenLaboratorioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrdenLaboratorio guardar(OrdenLaboratorio orden) {
        // Si la fila ya existe se actualiza (así se conserva @Version); si no, se inserta.
        OrdenLaboratorioEntity entidad = jpaRepository.findById(orden.getId())
                .map(existente -> {
                    existente.actualizarDesde(orden);
                    return existente;
                })
                .orElseGet(() -> OrdenLaboratorioEntity.desde(orden));
        return jpaRepository.save(entidad).aDominio();
    }

    @Override
    public Optional<OrdenLaboratorio> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(OrdenLaboratorioEntity::aDominio);
    }

    @Override
    public List<OrdenLaboratorio> listarTodas() {
        return jpaRepository.findAllByOrderByFechaSolicitudDesc().stream()
                .map(OrdenLaboratorioEntity::aDominio)
                .toList();
    }

    @Override
    public List<OrdenLaboratorio> listarPorPaciente(String pacienteId) {
        return jpaRepository.findByPacienteIdOrderByFechaSolicitudDesc(pacienteId).stream()
                .map(OrdenLaboratorioEntity::aDominio)
                .toList();
    }

    @Override
    public boolean existenPendientesDelPaciente(String pacienteId) {
        return jpaRepository.existsByPacienteIdAndEstadoIn(pacienteId, EstadoOrdenLaboratorio.pendientes());
    }
}
