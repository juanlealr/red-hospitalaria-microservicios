package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEvent;
import co.edu.uptc.admisiones_service.infraestructura.salida.persistencia.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdmisionService implements AdmisionUseCase {

    private final RepositorioAdmisiones repositorioAdmisiones;
    private final AdmisionFactory admisionFactory;
    private final TrasladoPacienteService trasladoPacienteService;
    private final VerificadorLaboratorioPuerto verificadorLaboratorio;
    private final ConsultaCoberturaExterna consultaCoberturaExterna;
    private final OutboxEventRepository outboxRepository;
    private final InventarioCamasUseCase inventarioCamasUseCase;
    private final ObjectMapper objectMapper;
    private final GeneradorIdPaciente generadorIdPaciente;

    public AdmisionService(
            RepositorioAdmisiones repositorioAdmisiones,
            AdmisionFactory admisionFactory,
            TrasladoPacienteService trasladoPacienteService,
            VerificadorLaboratorioPuerto verificadorLaboratorio,
            ConsultaCoberturaExterna consultaCoberturaExterna,
            OutboxEventRepository outboxRepository,
            InventarioCamasUseCase inventarioCamasUseCase,
            ObjectMapper objectMapper,
            GeneradorIdPaciente generadorIdPaciente) {
        this.repositorioAdmisiones = repositorioAdmisiones;
        this.admisionFactory = admisionFactory;
        this.trasladoPacienteService = trasladoPacienteService;
        this.verificadorLaboratorio = verificadorLaboratorio;
        this.consultaCoberturaExterna = consultaCoberturaExterna;
        this.outboxRepository = outboxRepository;
        this.inventarioCamasUseCase = inventarioCamasUseCase;
        this.objectMapper = objectMapper;
        this.generadorIdPaciente = generadorIdPaciente;
    }

    @Override
    @Transactional
    public Admision registrarIngreso(
            String pacienteId,
            String sedeId,
            String servicio,
            String medicoTratante,
            String motivoIngreso) {

        if (pacienteId == null || pacienteId.isBlank()) {
            pacienteId = generadorIdPaciente.siguiente();
        } else {
            pacienteId = pacienteId.trim();
        }

        if (pacienteId != null && repositorioAdmisiones.existeActivaDelPaciente(pacienteId)) {
            throw new IllegalStateException(
                    "El paciente %s ya tiene una admisión activa. Debe darse de alta antes de registrar un nuevo ingreso."
                            .formatted(pacienteId));
        }

        DisponibilidadCama disponibilidad = inventarioCamasUseCase.consultarDisponibilidad(sedeId, servicio);

        Admision admision = admisionFactory.registrarIngreso(
                pacienteId, sedeId, medicoTratante, motivoIngreso, disponibilidad);
        Admision guardada = repositorioAdmisiones.guardar(admision);

        inventarioCamasUseCase.ocuparCama(sedeId, servicio);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("admisionId", guardada.id());
        evento.put("pacienteId", guardada.pacienteId());
        evento.put("sedeInicialId", guardada.sedeActualId());
        evento.put("servicioInicial", guardada.servicioActual());
        evento.put("medicoTratante", guardada.medicoTratante());
        evento.put("motivoIngreso", guardada.motivoIngreso());
        evento.put("fechaIngreso", guardada.fechaIngreso().toString());

        outboxRepository.save(new OutboxEvent(guardada.pacienteId(), "PacienteIngresado", serializar(evento)));
        return guardada;
    }

    @Override
    @Transactional
    public Admision verificarCoberturaAdmision(String admisionId) {
        Admision admision = buscarPorId(admisionId);
        int porcentaje = consultaCoberturaExterna.consultarPorcentajeCobertura(admision.pacienteId());
        admision.registrarCobertura(porcentaje);
        Admision guardada = repositorioAdmisiones.guardar(admision);

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("admisionId", guardada.id());
        evento.put("pacienteId", guardada.pacienteId());
        evento.put("coberturaPorcentaje", porcentaje);
        evento.put("fechaVerificacion", LocalDateTime.now().toString());

        outboxRepository.save(
                new OutboxEvent(guardada.pacienteId(), "CoberturaPacienteVerificada", serializar(evento)));
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public int consultarCoberturaPaciente(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El id del paciente es obligatorio.");
        }
        return consultaCoberturaExterna.consultarPorcentajeCobertura(pacienteId.trim());
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

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("admisionId", admision.id());
        evento.put("pacienteId", admision.pacienteId());
        evento.put("sedeOrigenId", sedeOrigenId);
        evento.put("servicioOrigen", servicioOrigen);
        evento.put("sedeDestinoId", sedeDestinoId);
        evento.put("servicioDestino", servicioDestino);
        evento.put("fechaTraslado", fechaTraslado.toString());

        outboxRepository.save(new OutboxEvent(admision.pacienteId(), "PacienteTrasladado", serializar(evento)));
    }

    @Override
    @Transactional
    public void darDeAlta(String admisionId) {
        Admision admision = buscarPorId(admisionId);

        boolean ordenesPendientes = verificadorLaboratorio.tieneOrdenesPendientes(admision.pacienteId());

        LocalDateTime fechaAlta = LocalDateTime.now();
        admision.darDeAlta(ordenesPendientes, fechaAlta);
        repositorioAdmisiones.guardar(admision);

        inventarioCamasUseCase.liberarCama(admision.sedeActualId(), admision.servicioActual());

        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("admisionId", admision.id());
        evento.put("pacienteId", admision.pacienteId());
        evento.put("sedeId", admision.sedeActualId());
        evento.put("fechaAlta", fechaAlta.toString());

        outboxRepository.save(new OutboxEvent(admision.pacienteId(), "PacienteDadoDeAlta", serializar(evento)));
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

    private String serializar(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento de admisión.", e);
        }
    }
}