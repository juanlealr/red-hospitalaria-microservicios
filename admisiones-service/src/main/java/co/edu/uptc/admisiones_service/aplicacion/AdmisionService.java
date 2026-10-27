package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.infraestructura.salida.laboratorio.VerificadorLaboratorioGrpcAdapter;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEvent;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Caso de uso concreto: implementa AdmisionUseCase orquestando el Agregado,
 * la Factory, el Servicio de Dominio y el puerto secundario — equivalente
 * a InvestigadorService en RICA. Esta clase es la que faltaba en la
 * sección 6 del taller de DDD: ahí solo construimos las piezas tácticas
 * (Value Object, Servicio de Dominio, Agregado, Factory) sin nadie que las
 * orqueste ni las persista.
 */
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
    public Admision registrarIngreso(String pacienteId, String sedeId, DisponibilidadCama disponibilidadSede) {
        Admision admision = admisionFactory.registrarIngreso(pacienteId, sedeId, disponibilidadSede);
        return repositorioAdmisiones.guardar(admision);
    }

    @Override
    public void trasladarPaciente(String admisionId, DisponibilidadCama disponibilidadDestino) {
        Admision admision = buscarPorId(admisionId);
        trasladoPacienteService.trasladar(admision, disponibilidadDestino, LocalDateTime.now());
        repositorioAdmisiones.guardar(admision);
    }

    @Transactional
    public void darDeAlta(String admisionId, boolean tieneOrdenesLaboratorioPendientes) {
        Admision admision = repositorioAdmisiones.buscarPorId(admisionId)
                .orElseThrow(() -> new RuntimeException("Admisión no encontrada"));

        // 1. Lógica de dominio
        admision.darDeAlta(tieneOrdenesLaboratorioPendientes, LocalDateTime.now());
        repositorioAdmisiones.guardar(admision);

        // 2. Registro del evento en la tabla Outbox (misma transacción DB)
        String payload = """
                {"admisionId": "%s", "pacienteId": "%s", "fechaAlta": "%s"}"""
                .formatted(admision.id(), admision.pacienteId(), LocalDateTime.now());

        outboxRepository.save(new OutboxEvent(admision.pacienteId(), "PacienteDadoDeAlta", payload));
    }

    @Override
    public Admision buscarPorId(String admisionId) {
        return repositorioAdmisiones.buscarPorId(admisionId)
                .orElseThrow(() -> new NoSuchElementException("No existe una admisión con id " + admisionId));
    }

    @Override
    public List<Admision> listarTodas() {
        return repositorioAdmisiones.listarTodos();
    }
}
