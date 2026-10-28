package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto secundario (driven) del subdominio Farmacia y cobertura.
 *
 * Solo declara lo que el caso de uso necesita: guardar, buscar por id y
 * listar. No copia los métodos genéricos de JpaRepository; el núcleo define
 * el contrato y la infraestructura se adapta a él.
 */
public interface RepositorioSolicitudesMedicamento {

    SolicitudMedicamento guardar(SolicitudMedicamento solicitud);

    Optional<SolicitudMedicamento> buscarPorId(String id);

    List<SolicitudMedicamento> listarTodas();
}
