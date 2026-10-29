package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.Cobertura;
import co.edu.uptc.farmacia_service.dominio.EstadoSolicitudMedicamento;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificacionCoberturaServiceTest {

    @Test
    void consultaLaAseguradoraYRegistraLaCoberturaEnLaSolicitud() {
        var solicitud = new SolicitudMedicamento("sol-1", "pac-1", "med-1", "Ibuprofeno 400 mg", LocalDateTime.now());
        var verificacion = new VerificacionCoberturaService(pacienteId -> new Cobertura(80));

        var cobertura = verificacion.verificar(solicitud);

        assertEquals(new Cobertura(80), cobertura);
        assertEquals(EstadoSolicitudMedicamento.COBERTURA_VERIFICADA, solicitud.estado());
        assertEquals(new Cobertura(80), solicitud.cobertura());
    }

    @Test
    void consultaLaCoberturaDelPacienteDeLaSolicitud() {
        var solicitud = new SolicitudMedicamento("sol-1", "pac-42", "med-1", "Ibuprofeno 400 mg", LocalDateTime.now());
        var pacienteConsultado = new String[1];
        var verificacion = new VerificacionCoberturaService(pacienteId -> {
            pacienteConsultado[0] = pacienteId;
            return new Cobertura(50);
        });

        verificacion.verificar(solicitud);

        assertEquals("pac-42", pacienteConsultado[0]);
    }

    @Test
    void rechazaUnaSolicitudNula() {
        var verificacion = new VerificacionCoberturaService(pacienteId -> new Cobertura(80));

        assertThrows(IllegalArgumentException.class, () -> verificacion.verificar(null));
    }
}
