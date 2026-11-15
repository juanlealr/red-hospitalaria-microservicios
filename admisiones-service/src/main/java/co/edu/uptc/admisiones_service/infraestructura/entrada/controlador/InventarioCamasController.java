package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.admisiones_service.aplicacion.InventarioCamasUseCase;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import co.edu.uptc.admisiones_service.infraestructura.entrada.controlador.dto.RegistrarCapacidadRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import co.edu.uptc.admisiones_service.compartido.RecursoNoEncontradoException;
import co.edu.uptc.admisiones_service.infraestructura.entrada.controlador.dto.ReporteOcupacionSede;

import java.util.List;

@RestController
@RequestMapping("/api/admisiones/inventario-camas")
public class InventarioCamasController {

    private final InventarioCamasUseCase inventarioCamasUseCase;

    public InventarioCamasController(InventarioCamasUseCase inventarioCamasUseCase) {
        this.inventarioCamasUseCase = inventarioCamasUseCase;
    }

    /** HU-02: consultar la disponibilidad de camas por sede y servicio. */
    @GetMapping
    public ResponseEntity<DisponibilidadCama> consultarDisponibilidad(
            @RequestParam String sedeId, @RequestParam String servicio) {
        return ResponseEntity.ok(inventarioCamasUseCase.consultarDisponibilidad(sedeId, servicio));
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<DisponibilidadCama>> listarPorSede(@PathVariable String sedeId) {
        return ResponseEntity.ok(inventarioCamasUseCase.listarPorSede(sedeId));
    }

    /** Carga/actualiza la capacidad total de camas de una sede/servicio. */
    @PostMapping
    public ResponseEntity<DisponibilidadCama> registrarCapacidad(
            @Valid @RequestBody RegistrarCapacidadRequest request) {
        DisponibilidadCama disponibilidad = inventarioCamasUseCase.registrarCapacidad(
                request.sedeId(), request.servicio(), request.camasTotales());
        return ResponseEntity.status(HttpStatus.CREATED).body(disponibilidad);
    }

    /** HU-10: reporte de ocupación por sede, calculado sobre el inventario real. */
    @GetMapping("/reporte-ocupacion")
    public ResponseEntity<ReporteOcupacionSede> reporteOcupacion(@RequestParam String sedeId) {
        List<DisponibilidadCama> inventario = inventarioCamasUseCase.listarPorSede(sedeId);
        if (inventario.isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No hay inventario de camas registrado para la sede: " + sedeId);
        }
        return ResponseEntity.ok(ReporteOcupacionSede.desde(sedeId, inventario));
    }
}
