package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import org.springframework.stereotype.Service;

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

    public AdmisionService(
            RepositorioAdmisiones repositorioAdmisiones,
            AdmisionFactory admisionFactory,
            TrasladoPacienteService trasladoPacienteService) {
        this.repositorioAdmisiones = repositorioAdmisiones;
        this.admisionFactory = admisionFactory;
        this.trasladoPacienteService = trasladoPacienteService;
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

    @Override
    public void darDeAlta(String admisionId, boolean tieneOrdenesLaboratorioPendientes) {
        Admision admision = buscarPorId(admisionId);
        admision.darDeAlta(tieneOrdenesLaboratorioPendientes, LocalDateTime.now());
        repositorioAdmisiones.guardar(admision);
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
