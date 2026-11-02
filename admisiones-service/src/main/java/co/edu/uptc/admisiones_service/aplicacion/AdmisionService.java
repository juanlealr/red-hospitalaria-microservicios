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

    public AdmisionService(
            RepositorioAdmisiones repositorioAdmisiones,
            AdmisionFactory admisionFactory,
            TrasladoPacienteService trasladoPacienteService,
            VerificadorLaboratorioGrpcAdapter verificadorLaboratorioGrpc,
            OutboxEventRepository outboxRepository) {
        this.repositorioAdmisiones = repositorioAdmisiones;
        this.admisionFactory = admisionFactory;
        this.trasladoPacienteService = trasladoPacienteService;
        this.verificadorLaboratorioGrpc = verificadorLaboratorioGrpc;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public Admision registrarIngreso(String pacienteId, String sedeId, DisponibilidadCama disponibilidadSede) {
        Admision admision = admisionFactory.registrarIngreso(pacienteId, sedeId, disponibilidadSede);
        Admision guardada = repositorioAdmisiones.guardar(admision);

        // Evento de Dominio: PacienteIngresado
        String payload = """
                {
                    "admisionId": "%s",
                    "pacienteId": "%s",
                    "sedeInicialId": "%s",
                    "fechaIngreso": "%s"
                }""".formatted(guardada.id(), guardada.pacienteId(), guardada.sedeActualId(), guardada.fechaIngreso());

        outboxRepository.save(new OutboxEvent(guardada.pacienteId(), "PacienteIngresado", payload));
        return guardada;
    }

    @Override
    @Transactional
    public void trasladarPaciente(String admisionId, DisponibilidadCama disponibilidadDestino) {
        Admision admision = buscarPorId(admisionId);
        String sedeOrigenId = admision.sedeActualId();
        LocalDateTime fechaTraslado = LocalDateTime.now();

        trasladoPacienteService.trasladar(admision, disponibilidadDestino, fechaTraslado);
        repositorioAdmisiones.guardar(admision);

        // Evento de Dominio: PacienteTrasladado
        String payload = """
                {
                    "admisionId": "%s",
                    "pacienteId": "%s",
                    "sedeOrigenId": "%s",
                    "sedeDestinoId": "%s",
                    "fechaTraslado": "%s"
                }""".formatted(admision.id(), admision.pacienteId(), sedeOrigenId, disponibilidadDestino.sedeId(),
                fechaTraslado);

        outboxRepository.save(new OutboxEvent(admision.pacienteId(), "PacienteTrasladado", payload));
    }

    @Override
    @Transactional
    public void darDeAlta(String admisionId, boolean tieneOrdenesLaboratorioPendientesIgnorada) {
        Admision admision = buscarPorId(admisionId);

        // 1. Verificación síncrona gRPC con Resilience4j
        boolean ordenesPendientes = verificadorLaboratorioGrpc.tieneOrdenesPendientes(admision.pacienteId());

        // 2. Transición de estado en el Agregado
        LocalDateTime fechaAlta = LocalDateTime.now();
        admision.darDeAlta(ordenesPendientes, fechaAlta);
        repositorioAdmisiones.guardar(admision);

        // Evento de Dominio: PacienteDadoDeAlta
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
