package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.admisiones_service.aplicacion.AdmisionUseCase;
import co.edu.uptc.admisiones_service.dominio.Admision;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admisiones")
public class AdmisionController {

    private final AdmisionUseCase admisionUseCase;

    public AdmisionController(AdmisionUseCase admisionUseCase) {
        this.admisionUseCase = admisionUseCase;
    }

    @PostMapping("/ingreso")
    public ResponseEntity<Admision> registrarIngreso(
            @RequestParam String pacienteId,
            @RequestParam String sedeId,
            @RequestParam String servicio) {
        Admision admision = admisionUseCase.registrarIngreso(pacienteId, sedeId, servicio);
        return ResponseEntity.ok(admision);
    }

    @GetMapping
    public ResponseEntity<List<Admision>> listarTodas() {
        return ResponseEntity.ok(admisionUseCase.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Admision> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(admisionUseCase.buscarPorId(id));
    }

    @PutMapping("/{id}/traslado")
    public ResponseEntity<Void> trasladarPaciente(
            @PathVariable String id,
            @RequestParam String sedeDestinoId,
            @RequestParam String servicioDestino) {
        admisionUseCase.trasladarPaciente(id, sedeDestinoId, servicioDestino);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/alta")
    public ResponseEntity<Void> darDeAlta(@PathVariable String id) {
        admisionUseCase.darDeAlta(id);
        return ResponseEntity.noContent().build();
    }
}
