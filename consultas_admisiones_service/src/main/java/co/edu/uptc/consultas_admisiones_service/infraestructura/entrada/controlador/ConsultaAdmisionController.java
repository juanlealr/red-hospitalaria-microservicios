package co.edu.uptc.consultas_admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.consultas_admisiones_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.consultas_admisiones_service.dominio.modelo.FichaAdmision;
import co.edu.uptc.consultas_admisiones_service.dominio.repositorio.FichaAdmisionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consultas/admisiones")
public class ConsultaAdmisionController {

    private final FichaAdmisionRepository repositorio;

    public ConsultaAdmisionController(FichaAdmisionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public ResponseEntity<List<FichaAdmision>> listarTodas(@RequestParam(required = false) String estado) {
        if (estado != null) {
            return ResponseEntity.ok(repositorio.findByEstado(estado.toUpperCase()));
        }
        return ResponseEntity.ok(repositorio.findAll());
    }

    @GetMapping("/{admisionId}")
    public ResponseEntity<FichaAdmision> buscarPorId(@PathVariable String admisionId) {
        return repositorio.findById(admisionId)
                .map(ResponseEntity::ok)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("No existe ficha de admisión con ID: " + admisionId));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<FichaAdmision>> buscarPorPaciente(@PathVariable String pacienteId) {
        List<FichaAdmision> fichas = repositorio.findByPacienteId(pacienteId);
        return ResponseEntity.ok(fichas);
    }

    @GetMapping("/reporte/ocupacion")
    public ResponseEntity<Map<String, Object>> reporteOcupacion(@RequestParam String sedeId) {
        List<FichaAdmision> activas = repositorio.findBySedeActualIdAndEstado(sedeId, "ACTIVA");
        return ResponseEntity.ok(Map.of(
                "sedeId", sedeId,
                "camasOcupadasEnTiempoReal", activas.size(),
                "pacientesAdmitidos", activas));
    }
}
