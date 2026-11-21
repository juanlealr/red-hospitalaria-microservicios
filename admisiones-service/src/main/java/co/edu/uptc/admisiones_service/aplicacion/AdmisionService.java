package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.infraestructura.salida.laboratorio.VerificadorLaboratorioGrpcAdapter;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEvent;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdmisionService implements AdmisionUseCase {

    private final RepositorioAdmisiones repositorioAdmisiones;
    private final AdmisionFactory admisionFactory;
    private final TrasladoPacienteService trasladoPacienteService;
    private final VerificadorLaboratorioGrpcAdapter verificadorLaboratorioGrpc;
    private final OutboxEventRepository outboxRepository;
    private final InventarioCamasUseCase inventarioCamasUseCase;

    public AdmisionService(
            RepositorioAdmisiones repositorioAdmisiones,
            AdmisionFactory admisionFactory,
            TrasladoPacienteService trasladoPacienteService,
            VerificadorLaboratorioGrpcAdapter verificadorLaboratorioGrpc,
            OutboxEventRepository outboxRepository,
            InventarioCamasUseCase inventarioCamasUseCase) {
        this.repositorioAdmisiones = repositorioAdmisiones;
        this.admisionFactory = admisionFactory;
        this.trasladoPacienteService = trasladoPacienteService;
        this.verificadorLaboratorioGrpc = verificadorLaboratorioGrpc;
        this.outboxRepository = outboxRepository;
        this.inventarioCamasUseCase = inventarioCamasUseCase;
    }

    @Override
    @Transactional
    public Admision registrarIngreso(String pacienteId, String sedeId, String servicio) {

        if (pacienteId != null && repositorioAdmisiones.existeActivaDelPaciente(pacienteId)) {
            throw new IllegalStateException(
                    "El paciente %s ya tiene una admisión activa. Debe darse de alta antes de registrar un nuevo ingreso."
                            .formatted(pacienteId));
        }

        DisponibilidadCama disponibilidad = inventarioCamasUseCase.consultarDisponibilidad(sedeId, servicio);

        Admision admision = admisionFactory.registrarIngreso(pacienteId, sedeId, disponibilidad);
        Admision guardada = repositorioAdmisiones.guardar(admision);

        inventarioCamasUseCase.ocuparCama(sedeId, servicio);

        String payload = """
                {
                    "admisionId": "%s",
                    "pacienteId": "%s",
                    "sedeInicialId": "%s",
                    "servicioInicial": "%s",
                    "fechaIngreso": "%s"
                }""".formatted(guardada.id(), guardada.pacienteId(), guardada.sedeActualId(),
                guardada.servicioActual(), guardada.fechaIngreso());

        outboxRepository.save(new OutboxEvent(guardada.pacienteId(), "PacienteIngresado", payload));
        return guardada;
    }

    @Override
    @Transactional
    public void trasladarPaciente(String admisionId, String sedeDestinoId, String servicioDestino) {
        Admision admision = buscarPorId(admisionId);
        String sedeOrigenId = admision.sedeActualId();
        String servicioOrigen = admision.servicioActual();
        LocalDateTime fechaTraslado = LocalDateTime.now();

        DisponibilidadCama disponibilidadDestino = inventarioCamasUseCase.consultarDisponibilidad(sedeDestinoId,
                servicioDestino);

        trasladoPacienteService.trasladar(admision, disponibilidadDestino, fechaTraslado);
        repositorioAdmisiones.guardar(admision);

        inventarioCamasUseCase.liberarCama(sedeOrigenId, servicioOrigen);
        inventarioCamasUseCase.ocuparCama(sedeDestinoId, servicioDestino);

        String payload = """
                {
                    "admisionId": "%s",
                    "pacienteId": "%s",
                    "sedeOrigenId": "%s",
                    "sedeDestinoId": "%s",
                    "fechaTraslado": "%s"
                }""".formatted(admision.id(), admision.pacienteId(), sedeOrigenId, sedeDestinoId, fechaTraslado);

        outboxRepository.save(new OutboxEvent(admision.pacienteId(), "PacienteTrasladado", payload));
    }

    @Override
    @Transactional
    public void darDeAlta(String admisionId) {
        Admision admision = buscarPorId(admisionId);

        boolean ordenesPendientes = verificadorLaboratorioGrpc.tieneOrdenesPendientes(admision.pacienteId());

        LocalDateTime fechaAlta = LocalDateTime.now();
        admision.darDeAlta(ordenesPendientes, fechaAlta);
        repositorioAdmisiones.guardar(admision);

        inventarioCamasUseCase.liberarCama(admision.sedeActualId(), admision.servicioActual());

        String payload = """
                {
                    "admisionId": "%s",
                    "pacienteId": "%s",
                    "sedeId": "%s",
                    "fechaAlta": "%s"
                }""".formatted(admision.id(), admision.pacienteId(), admision.sedeActualId(), fechaAlta);

        outboxRepository.save(new OutboxEvent(admision.pacienteId(), "PacienteDadoDeAlta", payload));
    }

    @Override
    @Transactional(readOnly = true)
    public Admision buscarPorId(String admisionId) {
        return repositorioAdmisiones.buscarPorId(admisionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Admisión no encontrada con ID: " + admisionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admision> listarTodas() {
        return repositorioAdmisiones.listarTodos();
    }
}
