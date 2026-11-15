package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.dominio.InventarioCamas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GestionInventarioCamasService implements InventarioCamasUseCase {

    private final RepositorioInventarioCamas repositorio;

    public GestionInventarioCamasService(RepositorioInventarioCamas repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadCama consultarDisponibilidad(String sedeId, String servicio) {
        return buscarInventario(sedeId, servicio).aDisponibilidad();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadCama> listarPorSede(String sedeId) {
        return repositorio.listarPorSede(sedeId).stream()
                .map(InventarioCamas::aDisponibilidad)
                .toList();
    }

    @Override
    @Transactional
    public DisponibilidadCama registrarCapacidad(String sedeId, String servicio, int camasTotales) {
        InventarioCamas inventario = repositorio.buscarPorSedeYServicio(sedeId, servicio)
                .map(existente -> {
                    existente.actualizarCapacidad(camasTotales);
                    return existente;
                })
                .orElseGet(() -> new InventarioCamas(construirId(sedeId, servicio), sedeId, servicio, camasTotales));

        return repositorio.guardar(inventario).aDisponibilidad();
    }

    @Override
    @Transactional
    public DisponibilidadCama ocuparCama(String sedeId, String servicio) {
        InventarioCamas inventario = buscarInventario(sedeId, servicio);
        inventario.ocupar();
        return repositorio.guardar(inventario).aDisponibilidad();
    }

    @Override
    @Transactional
    public void liberarCama(String sedeId, String servicio) {
        InventarioCamas inventario = buscarInventario(sedeId, servicio);
        inventario.liberar();
        repositorio.guardar(inventario);
    }

    private InventarioCamas buscarInventario(String sedeId, String servicio) {
        return repositorio.buscarPorSedeYServicio(sedeId, servicio)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay inventario de camas registrado para sede %s / servicio %s. "
                                + "Regístralo primero con POST /api/admisiones/inventario-camas."
                                        .formatted(sedeId, servicio)));
    }

    private String construirId(String sedeId, String servicio) {
        return sedeId + "::" + servicio;
    }
}
