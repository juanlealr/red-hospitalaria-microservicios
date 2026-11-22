package co.edu.uptc.consultas_admisiones_service.dominio;

import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaAdmision;
import co.edu.uptc.consultas_admisiones_service.dominio.modelo.ResumenTraslado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FichaAdmisionTest {

    private FichaAdmision nuevaFicha() {
        return new FichaAdmision("A-1", "PAC-1001", "TUNJA", "URGENCIAS",
                "Dr. Prueba", "Control", "ACTIVA", "2026-11-21T10:00:00");
    }

    @Test
    void trasladoActualizaUbicacionYConservaElHistorial() {
        FichaAdmision ficha = nuevaFicha();

        boolean agregado = ficha.agregarTraslado(
                new ResumenTraslado("TUNJA", "URGENCIAS", "DUITAMA", "UCI", "2026-11-21T11:00:00"));
        ficha.agregarTraslado(
                new ResumenTraslado("DUITAMA", "UCI", "SOGAMOSO", "PEDIATRIA", "2026-11-21T12:00:00"));

        assertTrue(agregado);
        assertEquals("SOGAMOSO", ficha.getSedeActualId());
        assertEquals("PEDIATRIA", ficha.getServicioActual());
        assertEquals(2, ficha.getTotalTraslados());
        assertEquals(2, ficha.getTraslados().size());
    }

    @Test
    void ignoraUnTrasladoDuplicadoPorqueKafkaPuedeReentregar() {
        FichaAdmision ficha = nuevaFicha();
        ResumenTraslado traslado =
                new ResumenTraslado("TUNJA", "URGENCIAS", "DUITAMA", "UCI", "2026-11-21T11:00:00");

        assertTrue(ficha.agregarTraslado(traslado));
        assertFalse(ficha.agregarTraslado(traslado));
        assertEquals(1, ficha.getTotalTraslados());
    }

    @Test
    void darDeAltaCambiaElEstadoYGuardaLaFecha() {
        FichaAdmision ficha = nuevaFicha();

        ficha.darDeAlta("2026-11-22T09:00:00");

        assertEquals("DADA_DE_ALTA", ficha.getEstado());
        assertNotNull(ficha.getFechaAlta());
    }
}
