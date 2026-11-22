package co.edu.uptc.laboratorio_service.infraestructura.entrada.controlador.dto;

import co.edu.uptc.laboratorio_service.dominio.OrdenLaboratorio;

public record OrdenLaboratorioResponse(
        String id,
        String pacienteId,
        String tipoExamen,
        String estado,
        boolean pendiente,
        String fechaSolicitud,
        ResultadoResponse resultado) {

    public record ResultadoResponse(String valor, String unidad) {
    }

    public static OrdenLaboratorioResponse desde(OrdenLaboratorio orden) {
        ResultadoResponse resultado = orden.tieneResultado()
                ? new ResultadoResponse(orden.getResultado().valor(), orden.getResultado().unidad())
                : null;

        return new OrdenLaboratorioResponse(
                orden.getId().toString(),
                orden.getPacienteId(),
                orden.getTipoExamen(),
                orden.getEstado().name(),
                orden.estaPendiente(),
                String.valueOf(orden.getFechaSolicitud()),
                resultado);
    }
}
