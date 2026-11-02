package co.edu.uptc.consultas_admisiones_service.infraestructura.configuracion;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicosConfig {

    @Bean
    public NewTopic admisionesIngresosRegistrados() {
        return TopicBuilder.name("admisiones.ingresos-registrados").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic admisionesPacientesTrasladados() {
        return TopicBuilder.name("admisiones.pacientes-trasladados").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic admisionesPacienteDadoAlta() {
        return TopicBuilder.name("admisiones.paciente-dado-alta").partitions(3).replicas(1).build();
    }
}
