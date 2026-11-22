package co.edu.uptc.laboratorio_service.infraestructura.configuracion;

import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorioFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El dominio no conoce Spring: sus objetos se registran como beans desde aquí.
 */
@Configuration
public class DominioConfig {

    @Bean
    public OrdenLaboratorioFactory ordenLaboratorioFactory() {
        return new OrdenLaboratorioFactory();
    }
}
