package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TrasladoPacienteService {

    public void trasladar(Admision admision, DisponibilidadCama disponibilidadDestino, LocalDateTime fecha) {
        if (!disponibilidadDestino.hayCamaConfirmada()) {
            throw new IllegalStateException(
                    "No se puede trasladar: no hay cama confirmada disponible en la sede destino.");
        }
        admision.registrarTraslado(disponibilidadDestino.sedeId(), disponibilidadDestino.servicio(), fecha);
    }
}
