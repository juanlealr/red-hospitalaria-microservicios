package co.edu.uptc.admisiones_service.infraestructura.salida.persistencia;

import co.edu.uptc.admisiones_service.dominio.InventarioCamas;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioCamasJpaRepository extends JpaRepository<InventarioCamas, String> {
    Optional<InventarioCamas> findBySedeIdIgnoreCaseAndServicioIgnoreCase(String sedeId, String servicio);

    List<InventarioCamas> findBySedeIdIgnoreCase(String sedeId);
}
