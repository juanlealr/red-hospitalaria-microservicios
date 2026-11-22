package co.edu.uptc.laboratorio_service.dominio;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrdenLaboratorioFactory {

    public OrdenLaboratorio crear(
            String pacienteId,
            String tipoExamen
    ) {

        return new OrdenLaboratorio(
                UUID.randomUUID(),
                pacienteId,
                tipoExamen,
                LocalDateTime.now()
        );
    }
}
