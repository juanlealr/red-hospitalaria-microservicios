package co.edu.uptc.farmacia_service.dominio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoberturaTest {

    @Test
    void aceptaPorcentajesValidosEnLosLimites() {
        assertEquals(0, new Cobertura(0).porcentaje());
        assertEquals(100, new Cobertura(100).porcentaje());
    }

    @Test
    void aceptaUnPorcentajeIntermedio() {
        assertEquals(80, new Cobertura(80).porcentaje());
    }

    @Test
    void rechazaPorcentajeNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Cobertura(-1));
    }

    @Test
    void rechazaPorcentajeMayorA100() {
        assertThrows(IllegalArgumentException.class, () -> new Cobertura(101));
    }
}
