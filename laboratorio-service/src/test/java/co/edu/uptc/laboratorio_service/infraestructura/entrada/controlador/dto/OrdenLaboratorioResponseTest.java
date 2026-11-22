package co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto;

import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;
import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import co.edu.uptc.laboratorio_service.dominio.ResultadoExamen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El DTO de salida traduce el agregado a la forma JSON pública de la API. */
class OrdenLaboratorioResponseTest {

    private final OrdenLaboratorioFactory factory = new OrdenLaboratorioFactory();

    @Test
    void unaOrdenSolicitadaNoTraeResultadoYEstaPendiente() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");

        OrdenLaboratorioResponse dto = OrdenLaboratorioResponse.desde(orden);

        assertEquals(orden.getId().toString(), dto.id());
        assertEquals("PAC-1001", dto.pacienteId());
        assertEquals("Hemograma", dto.tipoExamen());
        assertEquals("SOLICITADA", dto.estado());
        assertTrue(dto.pendiente());
        assertNull(dto.resultado());
    }

    @Test
    void unaOrdenConResultadoLoExponeYSigueSiendoPendiente() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));

        OrdenLaboratorioResponse dto = OrdenLaboratorioResponse.desde(orden);

        assertEquals("CON_RESULTADO", dto.estado());
        assertTrue(dto.pendiente());
        assertNotNull(dto.resultado());
        assertEquals("120", dto.resultado().valor());
        assertEquals("g/L", dto.resultado().unidad());
    }

    @Test
    void unaOrdenCerradaDejaDeSerPendienteYConservaElResultado() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");
        orden.registrarResultado(new ResultadoExamen("120", "g/L"));
        orden.cerrar();

        OrdenLaboratorioResponse dto = OrdenLaboratorioResponse.desde(orden);

        assertEquals("CERRADA", dto.estado());
        assertFalse(dto.pendiente());
        assertEquals("120", dto.resultado().valor());
    }

    @Test
    void laFechaSeExponeComoTextoIsoIgualQueEnAdmisiones() {
        OrdenLaboratorio orden = factory.crear("PAC-1001", "Hemograma");

        OrdenLaboratorioResponse dto = OrdenLaboratorioResponse.desde(orden);

        assertEquals(orden.getFechaSolicitud().toString(), dto.fechaSolicitud());
    }
}
