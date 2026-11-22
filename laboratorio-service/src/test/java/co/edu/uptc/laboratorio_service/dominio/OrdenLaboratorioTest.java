package co.edu.uptc.laboratorio_service.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrdenLaboratorioTest {

    private final OrdenLaboratorioFactory factory = new OrdenLaboratorioFactory();

    @Test
    void laOrdenNaceSolicitadaYPendiente() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "  Hemograma ");

        assertEquals(EstadoOrdenLaboratorio.SOLICITADA, orden.getEstado());
        assertEquals("PAC-1001", orden.getPacienteId());
        assertEquals("Hemograma", orden.getTipoExamen());
        assertTrue(orden.estaPendiente());
        assertFalse(orden.tieneResultado());
    }

    @Test
    void rechazaPacienteOTipoDeExamenVacios() {
        assertThrows(IllegalArgumentException.class, () -> factory.crear(null, "Hemograma"));
        assertThrows(IllegalArgumentException.class, () -> factory.crear("  ", "Hemograma"));
        assertThrows(IllegalArgumentException.class, () -> factory.crear("PAC-1", null));
        assertThrows(IllegalArgumentException.class, () -> factory.crear("PAC-1", " "));
    }

    @Test
    void registrarResultadoLaDejaConResultadoPeroTodaviaPendiente() {
        OrdenLaboratorio orden = factory.crear("PAC-1", "Hemograma");

        orden.registrarResultado(new ResultadoExamen("120", "g/L"));

        assertEquals(EstadoOrdenLaboratorio.CON_RESULTADO, orden.getEstado());
        assertTrue(orden.estaPendiente());
    }

    @Test
    void cerrarSinResultadoFalla() {
        OrdenLaboratorio orden = factory.crear("PAC-1", "Hemograma");

        assertThrows(IllegalStateException.class, orden::cerrar);
    }

    @Test
    void cerrarLaDejaCerradaYYaNoEstaPendiente() {
        OrdenLaboratorio orden = factory.crear("PAC-1", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));

        orden.cerrar();

        assertEquals(EstadoOrdenLaboratorio.CERRADA, orden.getEstado());
        assertFalse(orden.estaPendiente());
    }

    @Test
    void noSeCierraDosVecesNiSeRegistraResultadoEnUnaOrdenCerrada() {
        OrdenLaboratorio orden = factory.crear("PAC-1", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));
        orden.cerrar();

        assertThrows(IllegalStateException.class, orden::cerrar);
        assertThrows(IllegalStateException.class,
                () -> orden.registrarResultado(new ResultadoExamen("130", "g/L")));
    }

    @Test
    void reconstituirAceptaEstadosCoherentes() {
        UUID id = UUID.randomUUID();
        LocalDateTime fecha = LocalDateTime.now();

        OrdenLaboratorio solicitada = OrdenLaboratorio.reconstituir(
                id, "PAC-1", "Hemograma", fecha, EstadoOrdenLaboratorio.SOLICITADA, null);
        OrdenLaboratorio cerrada = OrdenLaboratorio.reconstituir(
                id, "PAC-1", "Hemograma", fecha, EstadoOrdenLaboratorio.CERRADA,
                new ResultadoExamen("120", "g/L"));

        assertTrue(solicitada.estaPendiente());
        assertFalse(cerrada.estaPendiente());
        assertEquals(solicitada, cerrada, "Una entidad se compara por id");
    }

    @Test
    void reconstituirRechazaEstadosIncoherentes() {
        UUID id = UUID.randomUUID();
        LocalDateTime fecha = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> OrdenLaboratorio.reconstituir(
                id, "PAC-1", "Hemograma", fecha, EstadoOrdenLaboratorio.SOLICITADA,
                new ResultadoExamen("120", "g/L")));
        assertThrows(IllegalArgumentException.class, () -> OrdenLaboratorio.reconstituir(
                id, "PAC-1", "Hemograma", fecha, EstadoOrdenLaboratorio.CON_RESULTADO, null));
        assertThrows(NullPointerException.class, () -> OrdenLaboratorio.reconstituir(
                id, "PAC-1", "Hemograma", fecha, null, null));
    }

    @Test
    void resultadoExamenExigeValorYUnidadYLosRecorta() {
        assertThrows(IllegalArgumentException.class, () -> new ResultadoExamen(" ", "g/L"));
        assertThrows(IllegalArgumentException.class, () -> new ResultadoExamen("120", null));

        ResultadoExamen resultado = new ResultadoExamen(" 120 ", " g/L ");
        assertEquals("120", resultado.valor());
        assertEquals("g/L", resultado.unidad());
    }

    @Test
    void soloLasOrdenesSinCerrarCuentanComoPendientes() {
        assertEquals(
                java.util.List.of(EstadoOrdenLaboratorio.SOLICITADA, EstadoOrdenLaboratorio.CON_RESULTADO),
                EstadoOrdenLaboratorio.pendientes());
    }
}
