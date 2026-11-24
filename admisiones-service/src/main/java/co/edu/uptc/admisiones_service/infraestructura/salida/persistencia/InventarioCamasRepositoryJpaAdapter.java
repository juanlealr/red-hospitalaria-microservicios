package co.edu.uptc.admisiones_service.infraestructura.salida.persistencia;

import co.edu.uptc.admisiones_service.aplicacion.CatalogoCamasPuerto;
import co.edu.uptc.admisiones_service.aplicacion.RepositorioInventarioCamas;
import co.edu.uptc.admisiones_service.dominio.InventarioCamas;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class InventarioCamasRepositoryJpaAdapter implements RepositorioInventarioCamas, CatalogoCamasPuerto {

    private final InventarioCamasJpaRepository jpaRepository;

    public InventarioCamasRepositoryJpaAdapter(InventarioCamasJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<InventarioCamas> buscarPorSedeYServicio(String sedeId, String servicio) {
        return jpaRepository.findBySedeIdIgnoreCaseAndServicioIgnoreCase(sedeId, servicio);
    }

    @Override
    public List<InventarioCamas> listarPorSede(String sedeId) {
        return jpaRepository.findBySedeIdIgnoreCase(sedeId);
    }

    @Override
    public InventarioCamas guardar(InventarioCamas inventario) {
        return jpaRepository.save(inventario);
    }

    @Override
    public List<InventarioCamas> listarTodas() {
        return jpaRepository.findAll();
    }

}