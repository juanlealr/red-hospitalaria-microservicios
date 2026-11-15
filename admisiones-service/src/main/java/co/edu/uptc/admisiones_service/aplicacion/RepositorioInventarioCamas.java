package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.InventarioCamas;
import java.util.List;
import java.util.Optional;

public interface RepositorioInventarioCamas {
    Optional<InventarioCamas> buscarPorSedeYServicio(String sedeId, String servicio);

    List<InventarioCamas> listarPorSede(String sedeId);

    InventarioCamas guardar(InventarioCamas inventario);
}
