package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import java.util.List;

public interface InventarioCamasUseCase {
    DisponibilidadCama consultarDisponibilidad(String sedeId, String servicio);

    List<DisponibilidadCama> listarPorSede(String sedeId);

    DisponibilidadCama registrarCapacidad(String sedeId, String servicio, int camasTotales);

    // Usados internamente por AdmisionService — no son el foco de HU-02, pero
    // tienen que pasar por este mismo puerto para que el núcleo de Admision
    // nunca conozca InventarioCamasRepositoryJpaAdapter directamente.
    DisponibilidadCama ocuparCama(String sedeId, String servicio);

    void liberarCama(String sedeId, String servicio);
}
