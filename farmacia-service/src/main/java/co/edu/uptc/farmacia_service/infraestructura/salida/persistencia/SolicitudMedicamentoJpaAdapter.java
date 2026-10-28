package co.edu.uptc.farmacia_service.infraestructura.salida.persistencia;

import co.edu.uptc.farmacia_service.aplicacion.RepositorioSolicitudesMedicamento;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador secundario (driven) de persistencia: traduce entre el puerto
 * que diseñó el núcleo (RepositorioSolicitudesMedicamento) y el que genera
 * Spring Data JPA (SolicitudMedicamentoJpaRepository).
 */
@Component
public class SolicitudMedicamentoJpaAdapter implements RepositorioSolicitudesMedicamento {

    private final SolicitudMedicamentoJpaRepository repositorioJpa;

    public SolicitudMedicamentoJpaAdapter(SolicitudMedicamentoJpaRepository repositorioJpa) {
        this.repositorioJpa = repositorioJpa;
    }

    @Override
    public SolicitudMedicamento guardar(SolicitudMedicamento solicitud) {
        repositorioJpa.save(SolicitudMedicamentoPersistenceMapper.aEntidad(solicitud));
        return solicitud;
    }

    @Override
    public Optional<SolicitudMedicamento> buscarPorId(String id) {
        return repositorioJpa.findById(id).map(SolicitudMedicamentoPersistenceMapper::aDominio);
    }

    @Override
    public List<SolicitudMedicamento> listarTodas() {
        return repositorioJpa.findAll().stream()
                .map(SolicitudMedicamentoPersistenceMapper::aDominio)
                .toList();
    }
}
