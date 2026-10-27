package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.admisiones_service.aplicacion.AdmisionUseCase;
import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
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
            @RequestBody DisponibilidadCama disponibilidadSede) {
        Admision admision = admisionUseCase.registrarIngreso(pacienteId, sedeId, disponibilidadSede);
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
            @RequestBody DisponibilidadCama disponibilidadDestino) {
        admisionUseCase.trasladarPaciente(id, disponibilidadDestino);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/alta")
    public ResponseEntity<Void> darDeAlta(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean tieneOrdenesLaboratorioPendientes) {
        admisionUseCase.darDeAlta(id, tieneOrdenesLaboratorioPendientes);
        return ResponseEntity.noContent().build();
    }
}
