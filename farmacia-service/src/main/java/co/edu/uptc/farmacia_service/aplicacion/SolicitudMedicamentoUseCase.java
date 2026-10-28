package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;

import java.util.List;

/**
 * Puerto primario (driving) del subdominio Farmacia y cobertura.
 *
 * Cualquier adaptador primario (el controlador REST, una tarea programada o
 * una prueba) programa contra esta interfaz, nunca contra el caso de uso
 * concreto.
 */
public interface SolicitudMedicamentoUseCase {

    SolicitudMedicamento recibir(String pacienteId, String medicoId, String medicamento);

    SolicitudMedicamento verificarCobertura(String solicitudId);

    SolicitudMedicamento despachar(String solicitudId);

    SolicitudMedicamento buscarPorId(String solicitudId);

    List<SolicitudMedicamento> listarTodas();
}
