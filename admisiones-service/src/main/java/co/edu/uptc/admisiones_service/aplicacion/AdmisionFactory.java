package co.edu.uptc.admisiones_service.aplicacion;

import co.edu.uptc.admisiones_service.dominio.Admision;
import co.edu.uptc.admisiones_service.dominio.DisponibilidadCama;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Factory (paso 5 del taller de DDD): mueve aquí la validación y
 * construcción de la raíz del Agregado, igual que InvestigadorFactory en
 * el taller de RICA. Vive en aplicacion (no en dominio) porque, como
 * InvestigadorFactory, importa org.springframework.stereotype.Component —
 * ese es justo el criterio que da la guía de Hexagonal (sección 5, "la
 * prueba del núcleo limpio") para decidir dónde va.
 *
 * @Component: Spring la gestiona como bean (nada fuera de esta clase
 *             debería construir un Admision directamente).
 *
 *             Cubre HU-01: "registrar el ingreso de un paciente en una sede,
 *             para
 *             iniciar su atención" — y de paso aplica la regla de negocio de
 *             que no se
 *             puede admitir a alguien sin cama disponible confirmada en esa
 *             sede.
 *
 *             A diferencia de InvestigadorFactory, esta Factory NO recibe el
 *             Repository: no necesita comprobar duplicados (no hay una regla de
 *             "correo único" equivalente aquí), así que la disponibilidad de
 *             cama le
 *             llega ya resuelta por parámetro, y el id lo genera ella misma con
 *             UUID
 *             en vez de dejarlo en null para que la base de datos lo autogenere
 *             —
 *             decisión válida en JPA porque @Id no exige @GeneratedValue.
 */
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
        return new Admision(id, pacienteId, sedeId, LocalDateTime.now());
    }
}
