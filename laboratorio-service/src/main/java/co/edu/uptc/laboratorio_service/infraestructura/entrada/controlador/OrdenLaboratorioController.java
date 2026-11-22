package co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador;

import co.edu.uptc.laboratorio_service.aplicacion.OrdenLaboratorioUseCase;
import co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto.OrdenLaboratorioResponse;
import co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto.RegistrarResultadoRequest;
import co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto.SolicitarExamenRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/laboratorio/ordenes")
public class OrdenLaboratorioController {

    private final OrdenLaboratorioUseCase ordenUseCase;

    public OrdenLaboratorioController(OrdenLaboratorioUseCase ordenUseCase) {
        this.ordenUseCase = ordenUseCase;
    }

    public record OrdenesPendientesResponse(String pacienteId, boolean tienePendientes) {
    }

    /** HU-04 */
    @PostMapping
    public ResponseEntity<OrdenLaboratorioResponse> solicitarExamen(
            @Valid @RequestBody SolicitarExamenRequest request) {
        var orden = ordenUseCase.solicitarExamen(request.pacienteId(), request.tipoExamen());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrdenLaboratorioResponse.desde(orden));
    }

    @GetMapping
    public ResponseEntity<List<OrdenLaboratorioResponse>> listar(
            @RequestParam(required = false) String pacienteId) {
        return ResponseEntity.ok(ordenUseCase.listarOrdenes(pacienteId).stream()
                .map(OrdenLaboratorioResponse::desde)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenLaboratorioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(OrdenLaboratorioResponse.desde(ordenUseCase.buscarPorId(id)));
    }

    /** HU-09 (misma consulta que atiende el servidor gRPC, útil para depurar por HTTP). */
    @GetMapping("/pendientes/{pacienteId}")
    public ResponseEntity<OrdenesPendientesResponse> consultarPendientes(@PathVariable String pacienteId) {
        boolean pendientes = ordenUseCase.tieneOrdenesPendientes(pacienteId);
        return ResponseEntity.ok(new OrdenesPendientesResponse(pacienteId, pendientes));
    }

    /** HU-05 */
    @PostMapping("/{id}/resultado")
    public ResponseEntity<OrdenLaboratorioResponse> registrarResultado(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrarResultadoRequest request) {
        var orden = ordenUseCase.registrarResultado(id, request.valor(), request.unidad());
        return ResponseEntity.ok(OrdenLaboratorioResponse.desde(orden));
    }

    @PutMapping("/{id}/cierre")
    public ResponseEntity<Void> cerrar(@PathVariable UUID id) {
        ordenUseCase.cerrar(id);
        return ResponseEntity.noContent().build();
    }
}
