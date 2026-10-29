package co.edu.uptc.farmacia_service.aplicacion;

import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Caso de uso concreto de Farmacia y cobertura.
 *
 * Orquesta el Agregado, la Factory, el Servicio de Dominio de cobertura y
 * los puertos secundarios. No conoce HTTP, ni JPA, ni la tecnología
 * concreta de la aseguradora: todo entra por puertos.
 */
@Service
public class SolicitudMedicamentoService implements SolicitudMedicamentoUseCase {

    private final RepositorioSolicitudesMedicamento repositorio;
    private final SolicitudMedicamentoFactory factory;
    private final VerificacionCoberturaService verificacionCoberturaService;
    private final RegistroSolicitudTransaccional registro;

    public SolicitudMedicamentoService(
            RepositorioSolicitudesMedicamento repositorio,
            SolicitudMedicamentoFactory factory,
            VerificacionCoberturaService verificacionCoberturaService,
            RegistroSolicitudTransaccional registro) {
        this.repositorio = repositorio;
        this.factory = factory;
        this.verificacionCoberturaService = verificacionCoberturaService;
        this.registro = registro;
    }

    @Override
    public SolicitudMedicamento recibir(String pacienteId, String medicoId, String medicamento) {
        SolicitudMedicamento solicitud = factory.recibirSolicitud(pacienteId, medicoId, medicamento);
        return registro.guardar(solicitud, EventosFarmacia.MEDICAMENTO_SOLICITADO);
    }

    @Override
    public SolicitudMedicamento verificarCobertura(String solicitudId) {
        SolicitudMedicamento solicitud = buscarPorId(solicitudId);

        // Llamada de red fuera de la transacción: si la aseguradora falla,
        // la solicitud queda intacta en RECIBIDA.
        verificacionCoberturaService.verificar(solicitud);

        return registro.guardar(solicitud, EventosFarmacia.COBERTURA_VERIFICADA);
    }

    @Override
    public SolicitudMedicamento despachar(String solicitudId) {
        SolicitudMedicamento solicitud = buscarPorId(solicitudId);
        solicitud.despachar(LocalDateTime.now());
        return registro.guardar(solicitud, EventosFarmacia.MEDICAMENTO_DESPACHADO);
    }

    @Override
    public SolicitudMedicamento buscarPorId(String solicitudId) {
        if (solicitudId == null || solicitudId.isBlank()) {
            throw new IllegalArgumentException("El id de la solicitud es obligatorio.");
        }
        return repositorio.buscarPorId(solicitudId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe una solicitud de medicamento con id " + solicitudId));
    }

    @Override
    public List<SolicitudMedicamento> listarTodas() {
        return repositorio.listarTodas();
    }
}
