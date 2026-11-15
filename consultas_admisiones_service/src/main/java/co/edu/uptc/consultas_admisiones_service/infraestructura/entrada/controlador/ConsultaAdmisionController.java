package co.edu.uptc.consultas_admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.consultas_admisiones_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.consultas_admisiones_service.dominio.modelo.EstadoConsolidadoPaciente;
import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaAdmision;
import co.edu.uptc.consultas_admisiones_service.dominio.repositorio.FichaAdmisionRepository;
import co.edu.uptc.consultas_admisiones_service.dominio.repositorio.FichaMedicacionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consultas/admisiones")
public class ConsultaAdmisionController {

    private final FichaAdmisionRepository fichaAdmisionRepository;
    private final FichaMedicacionRepository fichaMedicacionRepository;

    public ConsultaAdmisionController(
            FichaAdmisionRepository fichaAdmisionRepository,
            FichaMedicacionRepository fichaMedicacionRepository) {
        this.fichaAdmisionRepository = fichaAdmisionRepository;
        this.fichaMedicacionRepository = fichaMedicacionRepository;
    }

    @GetMapping
    public ResponseEntity<List<FichaAdmision>> listarTodas(@RequestParam(required = false) String estado) {
        if (estado != null) {
            return ResponseEntity.ok(fichaAdmisionRepository.findByEstado(estado.toUpperCase()));
        }
        return ResponseEntity.ok(fichaAdmisionRepository.findAll());
    }

    @GetMapping("/{admisionId}")
    public ResponseEntity<FichaAdmision> buscarPorId(@PathVariable String admisionId) {
        return fichaAdmisionRepository.findById(admisionId)
                .map(ResponseEntity::ok)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("No existe ficha de admisión con ID: " + admisionId));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<FichaAdmision>> buscarPorPaciente(@PathVariable String pacienteId) {
        return ResponseEntity.ok(fichaAdmisionRepository.findByPacienteId(pacienteId));
    }

    /**
     * HU-08: estado consolidado del paciente (admisión + medicación; laboratorio se
     * suma después).
     */
    @GetMapping("/paciente/{pacienteId}/consolidado")
    public ResponseEntity<EstadoConsolidadoPaciente> consultarEstadoConsolidado(@PathVariable String pacienteId) {
        var admisiones = fichaAdmisionRepository.findByPacienteId(pacienteId);
        var medicaciones = fichaMedicacionRepository.findByPacienteId(pacienteId);
        return ResponseEntity.ok(new EstadoConsolidadoPaciente(pacienteId, admisiones, medicaciones));
    }

    @GetMapping("/reporte/ocupacion")
    public ResponseEntity<Map<String, Object>> reporteOcupacion(@RequestParam String sedeId) {
        var activas = fichaAdmisionRepository.findBySedeActualIdAndEstado(sedeId, "ACTIVA");
        return ResponseEntity.ok(Map.of(
                "sedeId", sedeId,
                "camasOcupadasEnTiempoReal", activas.size(),
                "pacientesAdmitidos", activas));
    }
}
