package co.edu.uptc.laboratorio_service.aplicacion;

import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada del contexto Laboratorio. Lo usan dos adaptadores de
 * entrada: el controlador REST y el servidor gRPC que consume Admisiones.
 */
public interface OrdenLaboratorioUseCase {

    /** HU-04: solicitar un examen de laboratorio. */
    OrdenLaboratorio solicitarExamen(String pacienteId, String tipoExamen);

    /** HU-05: registrar el resultado de un examen (propio o de equipo externo). */
    OrdenLaboratorio registrarResultado(UUID ordenId, String valor, String unidad);

    void cerrar(UUID ordenId);

    OrdenLaboratorio buscarPorId(UUID ordenId);

    /** Si pacienteId es nulo o vacío, lista todas las órdenes. */
    List<OrdenLaboratorio> listarOrdenes(String pacienteId);

    /** HU-09: lo consulta admisiones-service por gRPC antes de dar el alta. */
    boolean tieneOrdenesPendientes(String pacienteId);
}
