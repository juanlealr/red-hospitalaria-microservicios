package co.edu.uptc.consultas_admisiones_service.infraestructura.entrada.mensajeria;

import co.edu.uptc.consultas_admisiones_service.dominio.proyeccion.ProyectorFichasMedicacion;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProyeccionMedicacionListener {

    private final ProyectorFichasMedicacion proyector;

    public ProyeccionMedicacionListener(ProyectorFichasMedicacion proyector) {
        this.proyector = proyector;
    }

    @KafkaListener(topics = "farmacia.solicitudes")
    public void alRecibirEventoFarmacia(String mensaje) {
        proyector.alRecibirEventoFarmacia(mensaje);
    }
}
