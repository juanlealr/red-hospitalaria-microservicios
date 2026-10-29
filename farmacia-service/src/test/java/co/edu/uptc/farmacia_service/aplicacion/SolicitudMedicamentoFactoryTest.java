package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.EstadoSolicitudMedicamento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitudMedicamentoFactoryTest {

    private final SolicitudMedicamentoFactory factory = new SolicitudMedicamentoFactory();

    @Test
    void construyeUnaSolicitudCompletaYValida() {
        var solicitud = factory.recibirSolicitud("pac-1", "med-1", "Ibuprofeno 400 mg");

        assertNotNull(solicitud.id());
        assertEquals("pac-1", solicitud.pacienteId());
        assertEquals("med-1", solicitud.medicoId());
        assertEquals("Ibuprofeno 400 mg", solicitud.medicamento());
        assertEquals(EstadoSolicitudMedicamento.RECIBIDA, solicitud.estado());
        assertNotNull(solicitud.fechaSolicitud());
    }

    @Test
    void generaUnIdDistintoParaCadaSolicitud() {
        var primera = factory.recibirSolicitud("pac-1", "med-1", "Ibuprofeno 400 mg");
        var segunda = factory.recibirSolicitud("pac-1", "med-1", "Ibuprofeno 400 mg");

        assertNotEquals(primera.id(), segunda.id());
    }

    @Test
    void rechazaPacienteVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.recibirSolicitud(" ", "med-1", "Ibuprofeno 400 mg"));
    }

    @Test
    void rechazaSolicitudMedicaAusente() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.recibirSolicitud("pac-1", null, "Ibuprofeno 400 mg"));
    }

    @Test
    void rechazaMedicamentoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.recibirSolicitud("pac-1", "med-1", " "));
    }
}
