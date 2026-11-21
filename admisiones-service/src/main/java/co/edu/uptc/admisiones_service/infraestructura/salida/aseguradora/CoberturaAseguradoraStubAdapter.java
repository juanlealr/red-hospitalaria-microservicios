package co.edu.uptc.admisiones_service.infraestructura.salida.aseguradora;

import co.edu.uptc.admisiones_service.aplicacion.ConsultaCoberturaExterna;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!aseguradora-rest")
public class CoberturaAseguradoraStubAdapter implements ConsultaCoberturaExterna {

    @Override
    public int consultarPorcentajeCobertura(String pacienteId) {
        return 80;
    }
}
