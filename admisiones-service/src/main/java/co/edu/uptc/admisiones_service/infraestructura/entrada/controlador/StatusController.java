package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public ResponseEntity<Map<String, String>> obtenerEstado() {
        return ResponseEntity.ok(Map.of(
                "servicio", "admisiones-service",
                "estado", "UP"));
    }
}
