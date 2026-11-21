package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.admisiones_service.aplicacion.AdmisionUseCase;
import co.edu.uptc.admisiones_service.infraestructura.entrada.controlador.dto.AdmisionResponse;
import org.springframework.http.HttpStatus;
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

    public record CoberturaPacienteResponse(String pacienteId, int porcentajeCobertura) {
    }

    @PostMapping("/ingreso")
    public ResponseEntity<AdmisionResponse> registrarIngreso(
            @RequestParam String pacienteId,
            @RequestParam String sedeId,
            @RequestParam String servicio,
            @RequestParam String medicoTratante,
            @RequestParam String motivoIngreso) {
        var admision = admisionUseCase.registrarIngreso(
                pacienteId, sedeId, servicio, medicoTratante, motivoIngreso);
        return ResponseEntity.status(HttpStatus.CREATED).body(AdmisionResponse.desde(admision));
    }

    @GetMapping("/cobertura/{pacienteId}")
    public ResponseEntity<CoberturaPacienteResponse> consultarCoberturaPaciente(@PathVariable String pacienteId) {
        int porcentaje = admisionUseCase.consultarCoberturaPaciente(pacienteId);
        return ResponseEntity.ok(new CoberturaPacienteResponse(pacienteId, porcentaje));
    }

    @PostMapping("/{id}/verificar-cobertura")
    public ResponseEntity<AdmisionResponse> verificarCoberturaAdmision(@PathVariable String id) {
        var admision = admisionUseCase.verificarCoberturaAdmision(id);
        return ResponseEntity.ok(AdmisionResponse.desde(admision));
    }

    @GetMapping
    public ResponseEntity<List<AdmisionResponse>> listarTodas() {
        return ResponseEntity.ok(admisionUseCase.listarTodas().stream()
                .map(AdmisionResponse::desde)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdmisionResponse> buscarPorId(@PathVariable String id) {
        return ResponseEntity.ok(AdmisionResponse.desde(admisionUseCase.buscarPorId(id)));
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
