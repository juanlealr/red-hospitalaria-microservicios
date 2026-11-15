package co.edu.uptc.admisiones_service.infraestructura.entrada.controlador;

import co.edu.uptc.admisiones_service.aplicacion.ListadoCamasUseCase;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admisiones/inventario-camas")
public class ListadoCamasController {

    private final ListadoCamasUseCase listadoCamas;

    public ListadoCamasController(ListadoCamasUseCase listadoCamas) {
        this.listadoCamas = listadoCamas;
    }

    /**
     * HU-02: disponibilidad de todas las sedes y servicios (alimenta tabla y
     * desplegables).
     */
    @GetMapping("/todas")
    public ResponseEntity<List<DisponibilidadCama>> listarTodas() {
        return ResponseEntity.ok(listadoCamas.listarTodas());
    }
}