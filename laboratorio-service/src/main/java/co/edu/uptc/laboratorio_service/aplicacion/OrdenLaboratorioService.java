package co.edu.uptc.laboratorio_service.aplicacion;

import co.edu.uptc.laboratorio_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrdenLaboratorioService implements OrdenLaboratorioUseCase {

    private final RepositorioOrdenesLaboratorio repositorioOrdenes;
    private final OrdenLaboratorioFactory ordenFactory;
    private final PublicadorEventosOrden publicadorEventos;

    public OrdenLaboratorioService(
            RepositorioOrdenesLaboratorio repositorioOrdenes,
            OrdenLaboratorioFactory ordenFactory,
            PublicadorEventosOrden publicadorEventos) {
        this.repositorioOrdenes = repositorioOrdenes;
        this.ordenFactory = ordenFactory;
        this.publicadorEventos = publicadorEventos;
    }

    @Override
    @Transactional
    public OrdenLaboratorio solicitarExamen(String pacienteId, String tipoExamen) {
        OrdenLaboratorio orden = ordenFactory.crear(pacienteId, tipoExamen);
        OrdenLaboratorio guardada = repositorioOrdenes.guardar(orden);
        publicadorEventos.publicar(EventosLaboratorio.EXAMEN_SOLICITADO, guardada);
        return guardada;
    }

    @Override
    @Transactional
    public OrdenLaboratorio registrarResultado(UUID ordenId, String valor, String unidad) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        orden.registrarResultado(new ResultadoExamen(valor, unidad));
        OrdenLaboratorio guardada = repositorioOrdenes.guardar(orden);
        publicadorEventos.publicar(EventosLaboratorio.RESULTADO_REGISTRADO, guardada);
        return guardada;
    }

    @Override
    @Transactional
    public void cerrar(UUID ordenId) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        orden.cerrar();
        OrdenLaboratorio guardada = repositorioOrdenes.guardar(orden);
        publicadorEventos.publicar(EventosLaboratorio.ORDEN_CERRADA, guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenLaboratorio buscarPorId(UUID ordenId) {
        return buscarOrden(ordenId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenLaboratorio> listarOrdenes(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            return repositorioOrdenes.listarTodas();
        }
        return repositorioOrdenes.listarPorPaciente(pacienteId.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneOrdenesPendientes(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El id del paciente es obligatorio.");
        }
        return repositorioOrdenes.existenPendientesDelPaciente(pacienteId.trim());
    }

    private OrdenLaboratorio buscarOrden(UUID ordenId) {
        if (ordenId == null) {
            throw new IllegalArgumentException("El id de la orden es obligatorio.");
        }
        return repositorioOrdenes.buscarPorId(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Orden de laboratorio no encontrada con ID: " + ordenId));
    }
}
