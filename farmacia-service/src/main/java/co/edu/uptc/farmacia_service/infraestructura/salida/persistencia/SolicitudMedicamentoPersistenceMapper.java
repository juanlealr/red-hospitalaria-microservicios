package co.edu.uptc.farmacia_service.infraestructura.salida.persistencia;

import co.edu.uptc.farmacia_service.dominio.Cobertura;
import co.edu.uptc.farmacia_service.dominio.SolicitudMedicamento;

/**
 * Traduce entre el Agregado (núcleo) y el modelo JPA (infraestructura).
 * Es el único punto que conoce ambos modelos.
 */
public final class SolicitudMedicamentoPersistenceMapper {

    private SolicitudMedicamentoPersistenceMapper() {
    }

    public static SolicitudMedicamentoJpaEntity aEntidad(SolicitudMedicamento solicitud) {
        return new SolicitudMedicamentoJpaEntity(
                solicitud.id(),
                solicitud.pacienteId(),
                solicitud.medicoId(),
                solicitud.medicamento(),
                solicitud.fechaSolicitud(),
                solicitud.estado(),
                solicitud.cobertura() == null ? null : solicitud.cobertura().porcentaje(),
                solicitud.fechaDespacho());
    }

    public static SolicitudMedicamento aDominio(SolicitudMedicamentoJpaEntity entidad) {
        Cobertura cobertura = entidad.getCoberturaPorcentaje() == null
                ? null
                : new Cobertura(entidad.getCoberturaPorcentaje());

        return SolicitudMedicamento.reconstruir(
                entidad.getId(),
                entidad.getPacienteId(),
                entidad.getMedicoId(),
                entidad.getMedicamento(),
                entidad.getFechaSolicitud(),
                entidad.getEstado(),
                cobertura,
                entidad.getFechaDespacho());
    }
}
