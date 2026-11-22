package co.edu.uptc.laboratorio_service.aplicacion;

import co.edu.uptc.laboratorio_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.laboratorio_service.dominio.EstadoOrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Casos de uso probados con un fake escrito a mano: sin Spring, sin JPA, sin Mockito. */
class OrdenLaboratorioServiceTest {

    private final FakeRepositorioOrdenes repositorio = new FakeRepositorioOrdenes();
    private final OrdenLaboratorioService servicio =
            new OrdenLaboratorioService(repositorio, new OrdenLaboratorioFactory());

    @Test
    void solicitarExamenGuardaUnaOrdenSolicitada() {
        OrdenLaboratorio orden = servicio.solicitarExamen("PAC-1001", "Hemograma");

        assertEquals(EstadoOrdenLaboratorio.SOLICITADA, orden.getEstado());
        assertTrue(repositorio.buscarPorId(orden.getId()).isPresent());
    }

    @Test
    void flujoCompletoSolicitarRegistrarYCerrar() {
        UUID id = servicio.solicitarExamen("PAC-1001", "Hemograma").getId();

        OrdenLaboratorio conResultado = servicio.registrarResultado(id, "120", "g/L");
        assertEquals(EstadoOrdenLaboratorio.CON_RESULTADO, conResultado.getEstado());

        servicio.cerrar(id);
        assertEquals(EstadoOrdenLaboratorio.CERRADA, servicio.buscarPorId(id).getEstado());
    }

    @Test
    void cerrarSinResultadoSePropagaComoEstadoInvalido() {
        UUID id = servicio.solicitarExamen("PAC-1001", "Hemograma").getId();

        assertThrows(IllegalStateException.class, () -> servicio.cerrar(id));
    }

    @Test
    void ordenInexistenteLanzaRecursoNoEncontrado() {
        UUID noExiste = UUID.randomUUID();

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.buscarPorId(noExiste));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cerrar(noExiste));
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarResultado(noExiste, "120", "g/L"));
    }

    @Test
    void tieneOrdenesPendientesHastaQueTodasSeCierran() {
        assertFalse(servicio.tieneOrdenesPendientes("PAC-1001"));

        UUID id = servicio.solicitarExamen("PAC-1001", "Hemograma").getId();
        assertTrue(servicio.tieneOrdenesPendientes("PAC-1001"));

        servicio.registrarResultado(id, "120", "g/L");
        assertTrue(servicio.tieneOrdenesPendientes("PAC-1001"), "Con resultado pero sin cerrar sigue pendiente");

        servicio.cerrar(id);
        assertFalse(servicio.tieneOrdenesPendientes("PAC-1001"));
    }

    @Test
    void lasOrdenesPendientesSonPorPaciente() {
        servicio.solicitarExamen("PAC-1001", "Hemograma");

        assertFalse(servicio.tieneOrdenesPendientes("PAC-2002"));
    }

    @Test
    void consultarPendientesExigeElIdDelPaciente() {
        assertThrows(IllegalArgumentException.class, () -> servicio.tieneOrdenesPendientes(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.tieneOrdenesPendientes("  "));
    }

    @Test
    void listarOrdenesFiltraPorPacienteSiSeIndica() {
        servicio.solicitarExamen("PAC-1001", "Hemograma");
        servicio.solicitarExamen("PAC-1001", "Glucosa");
        servicio.solicitarExamen("PAC-2002", "Hemograma");

        assertEquals(3, servicio.listarOrdenes(null).size());
        assertEquals(3, servicio.listarOrdenes(" ").size());
        assertEquals(2, servicio.listarOrdenes("PAC-1001").size());
    }

    private static class FakeRepositorioOrdenes implements RepositorioOrdenesLaboratorio {

        private final Map<UUID, OrdenLaboratorio> ordenes = new LinkedHashMap<>();

        @Override
        public OrdenLaboratorio guardar(OrdenLaboratorio orden) {
            ordenes.put(orden.getId(), orden);
            return orden;
        }

        @Override
        public Optional<OrdenLaboratorio> buscarPorId(UUID id) {
            return Optional.ofNullable(ordenes.get(id));
        }

        @Override
        public List<OrdenLaboratorio> listarTodas() {
            return new ArrayList<>(ordenes.values());
        }

        @Override
        public List<OrdenLaboratorio> listarPorPaciente(String pacienteId) {
            return ordenes.values().stream()
                    .filter(o -> o.getPacienteId().equals(pacienteId))
                    .toList();
        }

        @Override
        public boolean existenPendientesDelPaciente(String pacienteId) {
            return ordenes.values().stream()
                    .anyMatch(o -> o.getPacienteId().equals(pacienteId) && o.estaPendiente());
        }
    }
}
