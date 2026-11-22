package co.edu.uptc.laboratorio_service.infraestructura.salida.persistencia;

import co.edu.uptc.laboratorio_service.dominio.EstadoOrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** El mapeo dominio <-> persistencia no debe perder información. */
class OrdenLaboratorioEntityTest {

    private final OrdenLaboratorioFactory factory = new OrdenLaboratorioFactory();

    @Test
    void ordenSolicitadaViajaIdaYVuelta() {
        OrdenLaboratorio original = factory.crear("PAC-1001", "Hemograma");

        OrdenLaboratorio restaurada = OrdenLaboratorioEntity.desde(original).aDominio();

        assertEquals(original, restaurada);
        assertEquals("PAC-1001", restaurada.getPacienteId());
        assertEquals("Hemograma", restaurada.getTipoExamen());
        assertEquals(original.getFechaSolicitud(), restaurada.getFechaSolicitud());
        assertEquals(EstadoOrdenLaboratorio.SOLICITADA, restaurada.getEstado());
        assertNull(restaurada.getResultado());
    }

    @Test
    void actualizarDesdeCopiaElResultadoYElEstado() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        OrdenLaboratorioEntity entidad = OrdenLaboratorioEntity.desde(orden);

        orden.registrarResultado(new ResultadoExamen("120", "g/L"));
        orden.cerrar();
        entidad.actualizarDesde(orden);

        OrdenLaboratorio restaurada = entidad.aDominio();
        assertEquals(EstadoOrdenLaboratorio.CERRADA, restaurada.getEstado());
        assertEquals(new ResultadoExamen("120", "g/L"), restaurada.getResultado());
    }
}
