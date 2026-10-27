package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio de Dominio (paso 3): TrasladoPacienteService.
 *
 * Cubre HU-03: "trasladar un paciente entre sedes o servicios, para
 * adecuar su atención a su condición". No pertenece naturalmente a
 * Admision (que no conoce la disponibilidad de otras sedes) ni a
 * DisponibilidadCama (que no conoce la Admision) — necesita a los dos,
 * por eso es un Servicio de Dominio y no un método de una sola entidad.
 *
 * @Service, igual que LimitePublicacionesAnualesService en el taller de
 * RICA: Spring lo gestiona como bean y se inyecta por constructor donde
 * haga falta — nunca se instancia con "new" fuera de los tests.
 *
 * Criterio de aceptación: el traslado se rechaza si no hay cama confirmada
 * disponible en el destino.
 */
@Service
public class TrasladoPacienteService {

    public void trasladar(Admision admision, DisponibilidadCama disponibilidadDestino, LocalDateTime fecha) {
        if (!disponibilidadDestino.hayCamaConfirmada()) {
            throw new IllegalStateException(
                    "No se puede trasladar: no hay cama confirmada disponible en la sede destino.");
        }
        admision.registrarTraslado(disponibilidadDestino.sedeId(), fecha);
    }
}
