package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class AdmisionFactory {

    public Admision registrarIngreso(String pacienteId, String sedeId, DisponibilidadCama disponibilidadSede) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("Se requiere el id del paciente para registrar el ingreso.");
        }
        if (sedeId == null || sedeId.isBlank()) {
            throw new IllegalArgumentException("Se requiere la sede de ingreso.");
        }
        if (!disponibilidadSede.sedeId().equals(sedeId)) {
            throw new IllegalArgumentException(
                    "La disponibilidad de camas consultada no corresponde a la sede de ingreso.");
        }
        if (!disponibilidadSede.hayCamaConfirmada()) {
            throw new IllegalStateException("No se puede registrar el ingreso: no hay cama disponible en la sede.");
        }

        String id = UUID.randomUUID().toString();
        return new Admision(id, pacienteId, sedeId, disponibilidadSede.servicio(), LocalDateTime.now());
    }
}
