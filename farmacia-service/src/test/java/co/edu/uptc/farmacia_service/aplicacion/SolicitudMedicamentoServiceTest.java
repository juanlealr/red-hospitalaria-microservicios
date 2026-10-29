package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.Cobertura;
import co.edu.uptc.farmacia_service.dominio.EstadoSolicitudMedicamento;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudMedicamentoServiceTest {

    private final FakeRepositorioSolicitudes repositorio = new FakeRepositorioSolicitudes();
    private final FakePublicadorEventos publicador = new FakePublicadorEventos();
    private final SolicitudMedicamentoService servicio = new SolicitudMedicamentoService(
            repositorio,
            new SolicitudMedicamentoFactory(),
            new VerificacionCoberturaService(pacienteId -> new Cobertura(80)),
            new RegistroSolicitudTransaccional(repositorio, publicador));

    @Test
    void recibeGuardaYPublicaMedicamentoSolicitado() {
        var solicitud = servicio.recibir("pac-1", "med-1", "Ibuprofeno 400 mg");

        assertNotNull(solicitud.id());
        assertEquals(EstadoSolicitudMedicamento.RECIBIDA, solicitud.estado());
        assertTrue(repositorio.buscarPorId(solicitud.id()).isPresent());
        assertEquals(List.of(EventosFarmacia.MEDICAMENTO_SOLICITADO), publicador.tipos);
    }

    @Test
    void verificaLaCoberturaYPublicaCoberturaVerificada() {
        var solicitud = servicio.recibir("pac-1", "med-1", "Ibuprofeno 400 mg");

        var actualizada = servicio.verificarCobertura(solicitud.id());

        assertEquals(EstadoSolicitudMedicamento.COBERTURA_VERIFICADA, actualizada.estado());
        assertEquals(new Cobertura(80), actualizada.cobertura());
        assertEquals(EventosFarmacia.COBERTURA_VERIFICADA, publicador.tipos.get(1));
    }

    @Test
    void despachaDespuesDeVerificarLaCobertura() {
        var solicitud = servicio.recibir("pac-1", "med-1", "Ibuprofeno 400 mg");
        servicio.verificarCobertura(solicitud.id());

        var despachada = servicio.despachar(solicitud.id());

        assertEquals(EstadoSolicitudMedicamento.DESPACHADA, despachada.estado());
        assertNotNull(despachada.fechaDespacho());
        assertEquals(EventosFarmacia.MEDICAMENTO_DESPACHADO, publicador.tipos.get(2));
    }

    @Test
    void noDespachaSinCoberturaVerificada() {
        var solicitud = servicio.recibir("pac-1", "med-1", "Ibuprofeno 400 mg");

        assertThrows(IllegalStateException.class, () -> servicio.despachar(solicitud.id()));
        assertEquals(List.of(EventosFarmacia.MEDICAMENTO_SOLICITADO), publicador.tipos);
    }

    @Test
    void siLaAseguradoraFallaLaSolicitudQuedaRecibidaYSinEvento() {
        var repositorioLocal = new FakeRepositorioSolicitudes();
        var publicadorLocal = new FakePublicadorEventos();
        var servicioConFallo = new SolicitudMedicamentoService(
                repositorioLocal,
                new SolicitudMedicamentoFactory(),
                new VerificacionCoberturaService(pacienteId -> {
                    throw new IllegalStateException("aseguradora caída");
                }),
                new RegistroSolicitudTransaccional(repositorioLocal, publicadorLocal));

        var solicitud = servicioConFallo.recibir("pac-1", "med-1", "Ibuprofeno 400 mg");

        assertThrows(IllegalStateException.class, () -> servicioConFallo.verificarCobertura(solicitud.id()));

        var sinCambios = repositorioLocal.buscarPorId(solicitud.id()).orElseThrow();
        assertEquals(EstadoSolicitudMedicamento.RECIBIDA, sinCambios.estado());
        assertEquals(List.of(EventosFarmacia.MEDICAMENTO_SOLICITADO), publicadorLocal.tipos);
    }

    @Test
    void fallaSiLaSolicitudNoExiste() {
        assertThrows(NoSuchElementException.class, () -> servicio.buscarPorId("no-existe"));
        assertThrows(NoSuchElementException.class, () -> servicio.verificarCobertura("no-existe"));
    }

    @Test
    void listaTodasLasSolicitudes() {
        servicio.recibir("pac-1", "med-1", "Ibuprofeno 400 mg");
        servicio.recibir("pac-2", "med-2", "Paracetamol 500 mg");

        assertEquals(2, servicio.listarTodas().size());
    }

    private static class FakeRepositorioSolicitudes implements RepositorioSolicitudesMedicamento {

        private final Map<String, SolicitudMedicamento> solicitudes = new HashMap<>();

        @Override
        public SolicitudMedicamento guardar(SolicitudMedicamento solicitud) {
            solicitudes.put(solicitud.id(), solicitud);
            return solicitud;
        }

        @Override
        public Optional<SolicitudMedicamento> buscarPorId(String id) {
            return Optional.ofNullable(solicitudes.get(id));
        }

        @Override
        public List<SolicitudMedicamento> listarTodas() {
            return new ArrayList<>(solicitudes.values());
        }
    }

    private static class FakePublicadorEventos implements PublicadorEventosSolicitud {

        private final List<String> tipos = new ArrayList<>();

        @Override
        public void publicar(String tipoEvento, SolicitudMedicamento solicitud) {
            tipos.add(tipoEvento);
        }
    }
}
