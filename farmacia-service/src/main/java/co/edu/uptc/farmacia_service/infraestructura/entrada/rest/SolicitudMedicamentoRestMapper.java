package co.edu.uptc.farmacia_service.infraestructura.entrada.rest;

import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;
import co.edu.uptc.farmacia_service.infraestructura.entrada.rest.dto.SolicitudMedicamentoResponse;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el Agregado del núcleo y el DTO del adaptador primario.
 * Es el único punto del adaptador que conoce ambos modelos.
 */
@Component
public class SolicitudMedicamentoRestMapper {

    public SolicitudMedicamentoResponse toResponse(SolicitudMedicamento solicitud) {
        return new SolicitudMedicamentoResponse(
                solicitud.id(),
                solicitud.pacienteId(),
                solicitud.medicoId(),
                solicitud.medicamento(),
                solicitud.estado().name(),
                solicitud.cobertura() == null ? null : solicitud.cobertura().porcentaje(),
                solicitud.fechaSolicitud(),
                solicitud.fechaDespacho());
    }
}
