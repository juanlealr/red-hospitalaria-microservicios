package co.edu.uptc.consultas_admisiones_service.infraestructura.entrada.mensajeria;

import co.edu.uptc.consultas_admisiones_service.dominio.proyeccion.ProyectorFichasAdmision;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProyeccionListener {

    private final ProyectorFichasAdmision proyector;

    public ProyeccionListener(ProyectorFichasAdmision proyector) {
        this.proyector = proyector;
    }

    @KafkaListener(topics = "admisiones.ingresos-registrados")
    public void pacienteIngresado(String mensaje) {
        proyector.alIngresarPaciente(mensaje);
    }

    @KafkaListener(topics = "admisiones.pacientes-trasladados")
    public void pacienteTrasladado(String mensaje) {
        proyector.alTrasladarPaciente(mensaje);
    }

    @KafkaListener(topics = "admisiones.paciente-dado-alta")
    public void pacienteDadoDeAlta(String mensaje) {
        proyector.alDarDeAltaPaciente(mensaje);
    }
}
